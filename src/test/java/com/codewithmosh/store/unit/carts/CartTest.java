package com.codewithmosh.store.unit.carts;

import com.codewithmosh.store.carts.Cart;
import com.codewithmosh.store.carts.CartItem;
import com.codewithmosh.store.products.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cart Entity")
class CartTest {

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Product buildProduct(Long id, BigDecimal price) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .price(price)
                .build();
    }

    private CartItem addItemToCart(Product product, int quantity) {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setCart(cart);
        cart.getItems().add(item);
        return item;
    }

    // -----------------------------------------------------------------------
    // isEmpty()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("isEmpty()")
    class IsEmpty {

        @Test
        @DisplayName("returns true when cart has no items")
        void returnsTrue_whenCartHasNoItems() {
            assertThat(cart.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("returns false when cart has at least one item")
        void returnsFalse_whenCartHasItems() {
            addItemToCart(buildProduct(1L, BigDecimal.TEN), 1);
            assertThat(cart.isEmpty()).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // getTotalPrice()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getTotalPrice()")
    class GetTotalPrice {

        @Test
        @DisplayName("returns zero for an empty cart")
        void returnsZero_whenCartIsEmpty() {
            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("returns sum of all item total prices")
        void returnsSumOfItemPrices() {
            // Product A: $10 x 2 = $20
            addItemToCart(buildProduct(1L, BigDecimal.valueOf(10)), 2);
            // Product B: $5 x 3 = $15
            addItemToCart(buildProduct(2L, BigDecimal.valueOf(5)), 3);

            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(35));
        }

        @Test
        @DisplayName("returns single item price when cart has one item")
        void returnsSingleItemPrice_whenCartHasOneItem() {
            addItemToCart(buildProduct(1L, BigDecimal.valueOf(25)), 1);
            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(25));
        }
    }

    // -----------------------------------------------------------------------
    // getItem(Long productId)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getItem()")
    class GetItem {

        @Test
        @DisplayName("returns the CartItem for an existing product")
        void returnsCartItem_whenProductExists() {
            Product product = buildProduct(1L, BigDecimal.TEN);
            CartItem added = addItemToCart(product, 1);

            CartItem found = cart.getItem(1L);

            assertThat(found).isNotNull();
            assertThat(found).isSameAs(added);
        }

        @Test
        @DisplayName("returns null when product is not in the cart")
        void returnsNull_whenProductNotInCart() {
            assertThat(cart.getItem(999L)).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // addItem(Product product)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("addItem()")
    class AddItem {

        @Test
        @DisplayName("adds a new item with quantity 1 when product not already in cart")
        void addsNewItem_whenProductNotInCart() {
            Product product = buildProduct(1L, BigDecimal.TEN);

            CartItem result = cart.addItem(product);

            assertThat(cart.getItems()).hasSize(1);
            assertThat(result.getQuantity()).isEqualTo(1);
            assertThat(result.getProduct()).isSameAs(product);
        }

        @Test
        @DisplayName("increments quantity when product already exists in cart")
        void incrementsQuantity_whenProductAlreadyInCart() {
            Product product = buildProduct(1L, BigDecimal.TEN);
            addItemToCart(product, 2);

            CartItem result = cart.addItem(product);

            assertThat(cart.getItems()).hasSize(1);
            assertThat(result.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("returns existing CartItem reference when product already in cart")
        void returnsExistingCartItem_whenProductAlreadyInCart() {
            Product product = buildProduct(1L, BigDecimal.TEN);
            CartItem existing = addItemToCart(product, 1);

            CartItem result = cart.addItem(product);

            assertThat(result).isSameAs(existing);
        }

        @Test
        @DisplayName("sets cart reference on new CartItem")
        void setsCartReference_onNewCartItem() {
            Product product = buildProduct(1L, BigDecimal.TEN);

            CartItem result = cart.addItem(product);

            assertThat(result.getCart()).isSameAs(cart);
        }
    }

    // -----------------------------------------------------------------------
    // deleteItem(Long productId)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("deleteItem()")
    class DeleteItem {

        @Test
        @DisplayName("removes the item when product exists in cart")
        void removesItem_whenProductExists() {
            Product product = buildProduct(1L, BigDecimal.TEN);
            addItemToCart(product, 1);

            cart.deleteItem(1L);

            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        @DisplayName("nullifies cart reference on the removed item")
        void nullifiesCartRef_whenItemRemoved() {
            Product product = buildProduct(1L, BigDecimal.TEN);
            CartItem item = addItemToCart(product, 1);

            cart.deleteItem(1L);

            assertThat(item.getCart()).isNull();
        }

        @Test
        @DisplayName("does nothing when product is not in cart")
        void doesNothing_whenProductNotInCart() {
            cart.deleteItem(999L);
            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        @DisplayName("removes only the targeted item, leaving others intact")
        void removesOnlyTargetItem() {
            Product p1 = buildProduct(1L, BigDecimal.TEN);
            Product p2 = buildProduct(2L, BigDecimal.ONE);
            addItemToCart(p1, 1);
            addItemToCart(p2, 1);

            cart.deleteItem(1L);

            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItem(2L)).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // clear()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("removes all items from the cart")
        void removesAllItems() {
            addItemToCart(buildProduct(1L, BigDecimal.TEN), 1);
            addItemToCart(buildProduct(2L, BigDecimal.ONE), 2);

            cart.clear();

            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        @DisplayName("does nothing when cart is already empty")
        void doesNothing_whenCartAlreadyEmpty() {
            cart.clear();
            assertThat(cart.getItems()).isEmpty();
        }
    }
}
