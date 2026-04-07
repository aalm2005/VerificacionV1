package com.codewithmosh.store.unit.carts;

import com.codewithmosh.store.carts.CartItem;
import com.codewithmosh.store.products.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartItem Entity")
class CartItemTest {

    // -----------------------------------------------------------------------
    // getTotalPrice()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTotalPrice() returns product price multiplied by quantity")
    void getTotalPrice_returnsProductPriceTimesQuantity() {
        Product product = Product.builder()
                .id(1L)
                .name("Widget")
                .price(BigDecimal.valueOf(12.50))
                .build();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(3);

        assertThat(item.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(37.50));
    }

    @Test
    @DisplayName("getTotalPrice() returns zero when quantity is 0")
    void getTotalPrice_returnsZero_whenQuantityIsZero() {
        Product product = Product.builder()
                .id(1L)
                .name("Widget")
                .price(BigDecimal.valueOf(10))
                .build();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(0);

        assertThat(item.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getTotalPrice() returns price when quantity is 1")
    void getTotalPrice_returnsPrice_whenQuantityIsOne() {
        Product product = Product.builder()
                .id(1L)
                .name("Widget")
                .price(BigDecimal.valueOf(99.99))
                .build();

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);

        assertThat(item.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
    }
}
