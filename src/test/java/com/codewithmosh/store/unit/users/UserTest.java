package com.codewithmosh.store.unit.users;

import com.codewithmosh.store.products.Product;
import com.codewithmosh.store.users.Address;
import com.codewithmosh.store.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .build();
    }

    // -----------------------------------------------------------------------
    // addAddress()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("addAddress()")
    class AddAddress {

        @Test
        @DisplayName("adds address to the user's list")
        void addsAddressToList() {
            Address address = Address.builder().id(10L).street("123 Main St").city("Springfield").build();

            user.addAddress(address);

            assertThat(user.getAddresses()).containsExactly(address);
        }

        @Test
        @DisplayName("sets user reference on the address")
        void setsUserOnAddress() {
            Address address = Address.builder().id(10L).street("123 Main St").build();

            user.addAddress(address);

            assertThat(address.getUser()).isSameAs(user);
        }

        @Test
        @DisplayName("can add multiple addresses")
        void canAddMultipleAddresses() {
            Address a1 = Address.builder().id(1L).street("Street 1").build();
            Address a2 = Address.builder().id(2L).street("Street 2").build();

            user.addAddress(a1);
            user.addAddress(a2);

            assertThat(user.getAddresses()).hasSize(2).containsExactly(a1, a2);
        }
    }

    // -----------------------------------------------------------------------
    // removeAddress()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("removeAddress()")
    class RemoveAddress {

        @Test
        @DisplayName("removes address from the user's list")
        void removesAddressFromList() {
            Address address = Address.builder().id(10L).street("123 Main St").build();
            user.addAddress(address);

            user.removeAddress(address);

            assertThat(user.getAddresses()).doesNotContain(address);
        }

        @Test
        @DisplayName("nullifies user reference on the removed address")
        void nullifiesUserOnAddress() {
            Address address = Address.builder().id(10L).street("123 Main St").build();
            user.addAddress(address);

            user.removeAddress(address);

            assertThat(address.getUser()).isNull();
        }

        @Test
        @DisplayName("does nothing when address is not in the list")
        void doesNothing_whenAddressNotInList() {
            Address address = Address.builder().id(99L).street("Unknown").build();

            user.removeAddress(address);

            assertThat(user.getAddresses()).isEmpty();
        }

        @Test
        @DisplayName("removes only the targeted address, leaving others")
        void removesOnlyTargetAddress() {
            Address a1 = Address.builder().id(1L).street("Street 1").build();
            Address a2 = Address.builder().id(2L).street("Street 2").build();
            user.addAddress(a1);
            user.addAddress(a2);

            user.removeAddress(a1);

            assertThat(user.getAddresses()).containsExactly(a2);
        }
    }

    // -----------------------------------------------------------------------
    // addFavoriteProduct()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("addFavoriteProduct()")
    class AddFavoriteProduct {

        @Test
        @DisplayName("adds product to favorites set")
        void addsProductToFavorites() {
            Product product = Product.builder().id(1L).name("Widget").price(BigDecimal.TEN).build();

            user.addFavoriteProduct(product);

            assertThat(user.getFavoriteProducts()).containsExactly(product);
        }

        @Test
        @DisplayName("does not add the same product twice (set semantics)")
        void doesNotDuplicate_sameProduct() {
            Product product = Product.builder().id(1L).name("Widget").price(BigDecimal.TEN).build();

            user.addFavoriteProduct(product);
            user.addFavoriteProduct(product);

            assertThat(user.getFavoriteProducts()).hasSize(1);
        }

        @Test
        @DisplayName("can add multiple distinct products")
        void addsMultipleDistinctProducts() {
            Product p1 = Product.builder().id(1L).name("Widget").price(BigDecimal.TEN).build();
            Product p2 = Product.builder().id(2L).name("Gadget").price(BigDecimal.ONE).build();

            user.addFavoriteProduct(p1);
            user.addFavoriteProduct(p2);

            assertThat(user.getFavoriteProducts()).hasSize(2).containsExactlyInAnyOrder(p1, p2);
        }
    }
}
