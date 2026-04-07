package com.codewithmosh.store.unit.auth;

import com.codewithmosh.store.auth.Jwt;
import com.codewithmosh.store.auth.JwtConfig;
import com.codewithmosh.store.auth.JwtService;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough!";
    private static final int ACCESS_EXPIRATION = 3600;   // 1 hour in seconds
    private static final int REFRESH_EXPIRATION = 86400; // 1 day in seconds

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtService jwtService;

    private SecretKey secretKey;

    private User testUser;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes());

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(ACCESS_EXPIRATION);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(REFRESH_EXPIRATION);

        testUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();
    }

    // -----------------------------------------------------------------------
    // generateAccessToken()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessToken {

        @Test
        @DisplayName("returns a non-null Jwt")
        void returnsNonNullJwt() {
            Jwt token = jwtService.generateAccessToken(testUser);
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("token contains the correct user ID")
        void tokenContainsCorrectUserId() {
            Jwt token = jwtService.generateAccessToken(testUser);
            assertThat(token.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("token has correct role")
        void tokenContainsCorrectRole() {
            Jwt token = jwtService.generateAccessToken(testUser);
            assertThat(token.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("token is not expired immediately after generation")
        void tokenIsNotExpiredImmediately() {
            Jwt token = jwtService.generateAccessToken(testUser);
            assertThat(token.isExpired()).isFalse();
        }

        @Test
        @DisplayName("token can be serialised to a compact JWT string")
        void tokenSerialisesToCompactString() {
            Jwt token = jwtService.generateAccessToken(testUser);
            String compact = token.toString();
            assertThat(compact).isNotBlank();
            assertThat(compact.split("\\.")).hasSize(3);
        }
    }

    // -----------------------------------------------------------------------
    // generateRefreshToken()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshToken {

        @Test
        @DisplayName("returns a non-null Jwt")
        void returnsNonNullJwt() {
            Jwt token = jwtService.generateRefreshToken(testUser);
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("token contains the correct user ID")
        void tokenContainsCorrectUserId() {
            Jwt token = jwtService.generateRefreshToken(testUser);
            assertThat(token.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("token is not expired immediately after generation")
        void tokenIsNotExpiredImmediately() {
            Jwt token = jwtService.generateRefreshToken(testUser);
            assertThat(token.isExpired()).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // parseToken()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("parseToken()")
    class ParseToken {

        @Test
        @DisplayName("returns Jwt for a valid token string")
        void returnsJwt_forValidToken() {
            String tokenString = jwtService.generateAccessToken(testUser).toString();

            Jwt parsed = jwtService.parseToken(tokenString);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("returns null for an invalid token string")
        void returnsNull_forInvalidToken() {
            Jwt parsed = jwtService.parseToken("not.a.valid.token");
            assertThat(parsed).isNull();
        }

        @Test
        @DisplayName("returns null for a blank string")
        void returnsNull_forBlankString() {
            Jwt parsed = jwtService.parseToken("   ");
            assertThat(parsed).isNull();
        }

        @Test
        @DisplayName("returns null for a token signed with a different key")
        void returnsNull_forTokenSignedWithDifferentKey() {
            SecretKey otherKey = Keys.hmacShaKeyFor("completely-different-secret-key!!".getBytes());
            when(jwtConfig.getSecretKey()).thenReturn(otherKey);
            JwtService otherService = new JwtService(jwtConfig);
            String foreignToken = otherService.generateAccessToken(testUser).toString();

            // restore original key
            when(jwtConfig.getSecretKey()).thenReturn(secretKey);
            JwtService sut = new JwtService(jwtConfig);

            Jwt parsed = sut.parseToken(foreignToken);
            assertThat(parsed).isNull();
        }
    }
}
