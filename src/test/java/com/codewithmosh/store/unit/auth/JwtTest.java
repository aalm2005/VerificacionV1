package com.codewithmosh.store.unit.auth;

import com.codewithmosh.store.auth.Jwt;
import com.codewithmosh.store.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Jwt Value Object")
class JwtTest {

    // 32-byte (256-bit) secret required by HMAC-SHA256
    private static final String SECRET = "test-secret-key-that-is-long-enough!";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        key = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Claims buildClaims(long expirationOffsetMs, Long userId, Role role) {
        return Jwts.claims()
                .subject(userId.toString())
                .add("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .build();
    }

    // -----------------------------------------------------------------------
    // isExpired()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("isExpired()")
    class IsExpired {

        @Test
        @DisplayName("returns false when token has not yet expired")
        void returnsFalse_whenTokenHasNotExpired() {
            Claims claims = buildClaims(60_000, 1L, Role.USER);
            Jwt jwt = new Jwt(claims, key);

            assertThat(jwt.isExpired()).isFalse();
        }

        @Test
        @DisplayName("returns true when token is already expired")
        void returnsTrue_whenTokenIsExpired() {
            Claims claims = buildClaims(-1_000, 1L, Role.USER);
            Jwt jwt = new Jwt(claims, key);

            assertThat(jwt.isExpired()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // getUserId()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getUserId()")
    class GetUserId {

        @Test
        @DisplayName("returns the user ID from claims subject")
        void returnsUserId() {
            Claims claims = buildClaims(60_000, 42L, Role.USER);
            Jwt jwt = new Jwt(claims, key);

            assertThat(jwt.getUserId()).isEqualTo(42L);
        }
    }

    // -----------------------------------------------------------------------
    // getRole()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getRole()")
    class GetRole {

        @Test
        @DisplayName("returns USER role from claims")
        void returnsUserRole() {
            Claims claims = buildClaims(60_000, 1L, Role.USER);
            Jwt jwt = new Jwt(claims, key);

            assertThat(jwt.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("returns ADMIN role from claims")
        void returnsAdminRole() {
            Claims claims = buildClaims(60_000, 1L, Role.ADMIN);
            Jwt jwt = new Jwt(claims, key);

            assertThat(jwt.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    // -----------------------------------------------------------------------
    // toString()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("returns a non-blank compact JWT string")
        void returnsCompactJwtString() {
            Claims claims = buildClaims(60_000, 1L, Role.USER);
            Jwt jwt = new Jwt(claims, key);

            String token = jwt.toString();

            assertThat(token).isNotBlank();
            // A compact JWT has exactly 3 parts separated by dots
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("produced token is parseable back to the same subject")
        void producedTokenIsParseable() {
            Claims claims = buildClaims(60_000, 7L, Role.ADMIN);
            Jwt jwt = new Jwt(claims, key);

            String token = jwt.toString();

            Claims parsed = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertThat(parsed.getSubject()).isEqualTo("7");
        }
    }
}
