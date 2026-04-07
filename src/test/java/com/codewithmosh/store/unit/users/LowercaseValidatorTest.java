package com.codewithmosh.store.unit.users;

import com.codewithmosh.store.users.LowercaseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LowercaseValidator")
class LowercaseValidatorTest {

    private LowercaseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LowercaseValidator();
    }

    @Test
    @DisplayName("returns true when value is null")
    void returnsTrue_whenValueIsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("returns true when value is already lowercase")
    void returnsTrue_whenValueIsLowercase() {
        assertThat(validator.isValid("alice@example.com", null)).isTrue();
    }

    @Test
    @DisplayName("returns true when value is an empty string")
    void returnsTrue_whenValueIsEmpty() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    @DisplayName("returns false when value contains uppercase letters")
    void returnsFalse_whenValueContainsUppercase() {
        assertThat(validator.isValid("Alice@example.com", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when value is all uppercase")
    void returnsFalse_whenValueIsAllUppercase() {
        assertThat(validator.isValid("ALICE@EXAMPLE.COM", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when value is mixed case")
    void returnsFalse_whenValueIsMixedCase() {
        assertThat(validator.isValid("Alice", null)).isFalse();
    }

    @Test
    @DisplayName("returns true for a lowercase string with numbers and symbols")
    void returnsTrue_forLowercaseWithNumbersAndSymbols() {
        assertThat(validator.isValid("user123@domain.co.uk", null)).isTrue();
    }
}
