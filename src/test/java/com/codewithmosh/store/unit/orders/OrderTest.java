package com.codewithmosh.store.unit.orders;

import com.codewithmosh.store.carts.Cart;
import com.codewithmosh.store.carts.CartItem;
import com.codewithmosh.store.orders.Order;
import com.codewithmosh.store.orders.PaymentStatus;
import com.codewithmosh.store.products.Product;
import com.codewithmosh.store.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order Entity")
class OrderTest {

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private User buildUser(Long id) {
        return User.builder().id(id).name("User " + id).email("user" + id + "@test.com").build();
    }

    private Cart buildCartWithItem(BigDecimal price, int quantity) {
        Product product = Product.builder()
                .id(1L)
                .name("Product")
                .price(price)
                .build();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setCart(null);

        Cart cart = new Cart();
        cart.getItems().add(item);
        item.setCart(cart);
        return cart;
    }

    // -----------------------------------------------------------------------
    // fromCart(Cart, User)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("fromCart()")
    class FromCart {

        @Test
        @DisplayName("sets customer on the order")
        void setsCustomer() {
            User customer = buildUser(1L);
            Cart cart = buildCartWithItem(BigDecimal.TEN, 1);

            Order order = Order.fromCart(cart, customer);

            assertThat(order.getCustomer()).isSameAs(customer);
        }

        @Test
        @DisplayName("sets status to PENDING")
        void setsStatusToPending() {
            Cart cart = buildCartWithItem(BigDecimal.TEN, 1);

            Order order = Order.fromCart(cart, buildUser(1L));

            assertThat(order.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("sets total price from cart")
        void setsTotalPriceFromCart() {
            // $10 x 2 = $20
            Cart cart = buildCartWithItem(BigDecimal.valueOf(10), 2);

            Order order = Order.fromCart(cart, buildUser(1L));

            assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(20));
        }

        @Test
        @DisplayName("creates OrderItems from CartItems")
        void createsOrderItemsFromCartItems() {
            Cart cart = buildCartWithItem(BigDecimal.TEN, 3);

            Order order = Order.fromCart(cart, buildUser(1L));

            assertThat(order.getItems()).hasSize(1);
            assertThat(order.getItems().iterator().next().getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("creates empty items set when cart is empty")
        void createsEmptyItems_whenCartIsEmpty() {
            Cart cart = new Cart();

            Order order = Order.fromCart(cart, buildUser(1L));

            assertThat(order.getItems()).isEmpty();
            assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // -----------------------------------------------------------------------
    // isPlacedBy(User)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("isPlacedBy()")
    class IsPlacedBy {

        @Test
        @DisplayName("returns true when order belongs to the given user")
        void returnsTrue_whenOrderBelongsToUser() {
            User customer = buildUser(1L);
            Cart cart = buildCartWithItem(BigDecimal.TEN, 1);
            Order order = Order.fromCart(cart, customer);

            assertThat(order.isPlacedBy(customer)).isTrue();
        }

        @Test
        @DisplayName("returns false when order belongs to a different user")
        void returnsFalse_whenOrderBelongsToDifferentUser() {
            User customer = buildUser(1L);
            User otherUser = buildUser(2L);
            Cart cart = buildCartWithItem(BigDecimal.TEN, 1);
            Order order = Order.fromCart(cart, customer);

            assertThat(order.isPlacedBy(otherUser)).isFalse();
        }
    }
}
