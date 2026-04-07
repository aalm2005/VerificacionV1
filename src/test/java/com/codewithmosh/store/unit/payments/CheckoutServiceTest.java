package com.codewithmosh.store.unit.payments;

import com.codewithmosh.store.auth.AuthService;
import com.codewithmosh.store.carts.Cart;
import com.codewithmosh.store.carts.CartEmptyException;
import com.codewithmosh.store.carts.CartNotFoundException;
import com.codewithmosh.store.carts.CartRepository;
import com.codewithmosh.store.carts.CartService;
import com.codewithmosh.store.orders.Order;
import com.codewithmosh.store.orders.OrderRepository;
import com.codewithmosh.store.orders.PaymentStatus;
import com.codewithmosh.store.payments.*;
import com.codewithmosh.store.products.Product;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import com.codewithmosh.store.carts.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutService")
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuthService authService;

    @Mock
    private CartService cartService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private CheckoutService checkoutService;

    private UUID cartId;
    private Cart cart;
    private User customer;
    private CheckOutRequest request;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID();

        customer = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();

        // Build a cart with one item so it is not empty
        Product product = Product.builder()
                .id(1L)
                .name("Widget")
                .price(BigDecimal.valueOf(20))
                .build();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);

        cart = new Cart();
        cart.getItems().add(item);
        item.setCart(cart);

        request = new CheckOutRequest();
        request.setCartId(cartId);
    }

    // -----------------------------------------------------------------------
    // checkout()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkout()")
    class Checkout {

        @Test
        @DisplayName("creates order, clears cart, and returns CheckOutResponse on success")
        void createsOrderAndReturnsResponse_onSuccess() {
            CheckoutSession session = new CheckoutSession("https://pay.stripe.com/session123");

            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            when(authService.getCurrentUser()).thenReturn(customer);
            when(paymentGateway.createCheckoutSession(any(Order.class))).thenReturn(session);

            // Capture saved order to verify its ID is returned
            doAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(42L);
                return null;
            }).when(orderRepository).save(any(Order.class));

            CheckOutResponse response = checkoutService.checkout(request);

            assertThat(response.getOrderId()).isEqualTo(42L);
            assertThat(response.getCheckoutUrl()).isEqualTo("https://pay.stripe.com/session123");
            verify(cartService).clearCart(cartId);
        }

        @Test
        @DisplayName("throws CartNotFoundException when cart does not exist")
        void throwsCartNotFoundException_whenCartNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkoutService.checkout(request))
                    .isInstanceOf(CartNotFoundException.class);

            verifyNoInteractions(orderRepository, paymentGateway);
        }

        @Test
        @DisplayName("throws CartEmptyException when cart has no items")
        void throwsCartEmptyException_whenCartIsEmpty() {
            Cart emptyCart = new Cart(); // no items
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(emptyCart));

            assertThatThrownBy(() -> checkoutService.checkout(request))
                    .isInstanceOf(CartEmptyException.class);

            verifyNoInteractions(orderRepository, paymentGateway);
        }

        @Test
        @DisplayName("deletes order and rethrows PaymentException when payment gateway fails")
        void deletesOrderAndRethrows_whenPaymentFails() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            when(authService.getCurrentUser()).thenReturn(customer);
            when(paymentGateway.createCheckoutSession(any(Order.class)))
                    .thenThrow(new PaymentException("stripe error"));

            assertThatThrownBy(() -> checkoutService.checkout(request))
                    .isInstanceOf(PaymentException.class);

            verify(orderRepository).delete(any(Order.class));
            verify(cartService, never()).clearCart(any());
        }
    }

    // -----------------------------------------------------------------------
    // handleWebhookRequest()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("handleWebhookRequest()")
    class HandleWebhookRequest {

        @Test
        @DisplayName("updates order status when payment result is present")
        void updatesOrderStatus_whenPaymentResultIsPresent() {
            Order order = new Order();
            order.setId(10L);
            order.setStatus(PaymentStatus.PENDING);

            PaymentResult result = new PaymentResult(10L, PaymentStatus.PAID);
            WebhookRequest webhookRequest = new WebhookRequest(null, null);

            when(paymentGateway.parseWebhookRequest(webhookRequest))
                    .thenReturn(Optional.of(result));
            when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

            checkoutService.handleWebhookRequest(webhookRequest);

            assertThat(order.getStatus()).isEqualTo(PaymentStatus.PAID);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("does not touch the order when payment result is empty")
        void doesNothing_whenPaymentResultIsEmpty() {
            WebhookRequest webhookRequest = new WebhookRequest(null, null);
            when(paymentGateway.parseWebhookRequest(webhookRequest))
                    .thenReturn(Optional.empty());

            checkoutService.handleWebhookRequest(webhookRequest);

            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("el sistema lanza un error cuando la notificación de pago referencia un pedido que ya no existe en la base de datos")
        void lanzaError_cuandoPedidoReferenciadoEnNotificacionNoExiste() {
            PaymentResult resultado     = new PaymentResult(999L, PaymentStatus.PAID);
            WebhookRequest notificacion = new WebhookRequest(null, null);

            when(paymentGateway.parseWebhookRequest(notificacion))
                    .thenReturn(Optional.of(resultado));
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // el .orElseThrow() en CheckoutService lanza NoSuchElementException cuando el pedido no existe
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> checkoutService.handleWebhookRequest(notificacion))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }
}
