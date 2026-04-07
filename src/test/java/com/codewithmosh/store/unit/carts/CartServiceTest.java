package com.codewithmosh.store.unit.carts;

import com.codewithmosh.store.carts.*;
import com.codewithmosh.store.products.Product;
import com.codewithmosh.store.products.ProductNotFoundException;
import com.codewithmosh.store.products.ProductRepository;
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
@DisplayName("CartService")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private UUID cartId;
    private Cart cart;
    private CartDto cartDto;
    private Product product;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID();

        cart = new Cart();

        cartDto = new CartDto();
        cartDto.setId(cartId);

        product = Product.builder()
                .id(1L)
                .name("Widget")
                .price(BigDecimal.valueOf(10))
                .build();
    }

    // -----------------------------------------------------------------------
    // createCart()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createCart()")
    class CreateCart {

        @Test
        @DisplayName("saves a new cart and returns its DTO")
        void savesCartAndReturnsDto() {
            when(cartMapper.toDto(any(Cart.class))).thenReturn(cartDto);

            CartDto result = cartService.createCart();

            verify(cartRepository).save(any(Cart.class));
            assertThat(result).isSameAs(cartDto);
        }
    }

    // -----------------------------------------------------------------------
    // getCart()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getCart()")
    class GetCart {

        @Test
        @DisplayName("returns CartDto when cart exists")
        void returnsCartDto_whenCartExists() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            when(cartMapper.toDto(cart)).thenReturn(cartDto);

            CartDto result = cartService.getCart(cartId);

            assertThat(result).isSameAs(cartDto);
        }

        @Test
        @DisplayName("throws CartNotFoundException when cart does not exist")
        void throwsCartNotFoundException_whenCartNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCart(cartId))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // addToCart()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("addToCart()")
    class AddToCart {

        @Test
        @DisplayName("adds product to cart and returns CartItemDto")
        void addsProductAndReturnsDto() {
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(1);
            cartItem.setCart(cart);

            CartItemDto cartItemDto = new CartItemDto();

            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartMapper.toDto(any(CartItem.class))).thenReturn(cartItemDto);

            CartItemDto result = cartService.addToCart(cartId, 1L);

            verify(cartRepository).save(cart);
            assertThat(result).isSameAs(cartItemDto);
        }

        @Test
        @DisplayName("throws CartNotFoundException when cart does not exist")
        void throwsCartNotFoundException_whenCartNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(cartId, 1L))
                    .isInstanceOf(CartNotFoundException.class);
        }

        @Test
        @DisplayName("throws ProductNotFoundException when product does not exist")
        void throwsProductNotFoundException_whenProductNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(cartId, 99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateItem()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("updateItem()")
    class UpdateItem {

        @Test
        @DisplayName("updates item quantity and returns CartItemDto")
        void updatesQuantityAndReturnsDto() {
            // Pre-load cart with one item
            CartItem existingItem = new CartItem();
            existingItem.setProduct(product);
            existingItem.setQuantity(1);
            existingItem.setCart(cart);
            cart.getItems().add(existingItem);

            CartItemDto updatedDto = new CartItemDto();

            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            when(cartMapper.toDto(existingItem)).thenReturn(updatedDto);

            CartItemDto result = cartService.updateItem(cartId, 1L, 5);

            assertThat(existingItem.getQuantity()).isEqualTo(5);
            verify(cartRepository).save(cart);
            assertThat(result).isSameAs(updatedDto);
        }

        @Test
        @DisplayName("throws CartNotFoundException when cart does not exist")
        void throwsCartNotFoundException_whenCartNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateItem(cartId, 1L, 3))
                    .isInstanceOf(CartNotFoundException.class);
        }

        @Test
        @DisplayName("throws ProductNotFoundException when item not found in cart")
        void throwsProductNotFoundException_whenItemNotInCart() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
            // cart is empty, so getItem(99L) returns null

            assertThatThrownBy(() -> cartService.updateItem(cartId, 99L, 3))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // deleteItem()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("deleteItem()")
    class DeleteItem {

        @Test
        @DisplayName("removes item from cart and saves")
        void removesItemAndSaves() {
            CartItem existingItem = new CartItem();
            existingItem.setProduct(product);
            existingItem.setQuantity(2);
            existingItem.setCart(cart);
            cart.getItems().add(existingItem);

            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

            cartService.deleteItem(cartId, 1L);

            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("throws CartNotFoundException when cart does not exist")
        void throwsCartNotFoundException_whenCartNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.deleteItem(cartId, 1L))
                    .isInstanceOf(CartNotFoundException.class);
        }

        @Test
        @DisplayName("el carrito no cambia y se guarda igual cuando el producto no estaba en el carrito — documenta que la operación es silenciosa")
        void carritoNoEsModificado_cuandoProductoNoEstaba() {
            // el carrito existe pero está vacío: el producto 99L no está en él
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

            cartService.deleteItem(cartId, 99L);

            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }
    }

    // -----------------------------------------------------------------------
    // clearCart()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("clearCart()")
    class ClearCart {

        @Test
        @DisplayName("clears all items from cart and saves")
        void clearsAllItemsAndSaves() {
            CartItem item1 = new CartItem();
            item1.setProduct(product);
            item1.setQuantity(1);
            item1.setCart(cart);
            cart.getItems().add(item1);

            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

            cartService.clearCart(cartId);

            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("throws CartNotFoundException when cart does not exist")
        void throwsCartNotFoundException_whenCartNotFound() {
            when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.clearCart(cartId))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }
}
