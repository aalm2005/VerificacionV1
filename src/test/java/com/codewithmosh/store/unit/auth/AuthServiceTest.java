package com.codewithmosh.store.unit.auth;

import com.codewithmosh.store.auth.AuthService;
import com.codewithmosh.store.auth.Jwt;
import com.codewithmosh.store.auth.JwtService;
import com.codewithmosh.store.auth.LoginRequest;
import com.codewithmosh.store.auth.LoginResponse;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import com.codewithmosh.store.users.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor("test-secret-key-that-is-long-enough!".getBytes());

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // Helper – build a Jwt wrapping real Claims
    // -----------------------------------------------------------------------

    private Jwt buildJwt(Long userId, long expirationOffsetMs) {
        Claims claims = Jwts.claims()
                .subject(userId.toString())
                .add("role", Role.USER.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .build();
        return new Jwt(claims, KEY);
    }

    // -----------------------------------------------------------------------
    // getCurrentUser()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUser {

        @Test
        @DisplayName("returns user when principal is a valid user ID")
        void returnsUser_whenAuthenticatedUserExists() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(testUser.getId());

            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            User result = authService.getCurrentUser();

            assertThat(result).isSameAs(testUser);
        }

        @Test
        @DisplayName("returns null when user ID is not found in the repository")
        void returnsNull_whenUserNotFound() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(99L);

            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            User result = authService.getCurrentUser();

            assertThat(result).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // login()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns LoginResponse with access and refresh tokens on success")
        void returnsLoginResponse_onSuccess() {
            LoginRequest request = new LoginRequest();
            request.setEmail("alice@example.com");
            request.setPassword("secret");

            Jwt accessJwt = buildJwt(1L, 3_600_000);
            Jwt refreshJwt = buildJwt(1L, 86_400_000);

            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn(accessJwt);
            when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshJwt);

            LoginResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isSameAs(accessJwt);
            assertThat(response.getRefreshToken()).isSameAs(refreshJwt);
            verify(jwtService).generateAccessToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("throws NoSuchElementException when user is not found in repository after successful authentication")
        void throwsNoSuchElement_whenUserNotFoundAfterAuthentication() {
            LoginRequest request = new LoginRequest();
            request.setEmail("ghost@example.com");
            request.setPassword("secret");

            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("delegates authentication to AuthenticationManager")
        void delegatesToAuthenticationManager() {
            LoginRequest request = new LoginRequest();
            request.setEmail("alice@example.com");
            request.setPassword("secret");

            when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(any())).thenReturn(buildJwt(1L, 3_600_000));
            when(jwtService.generateRefreshToken(any())).thenReturn(buildJwt(1L, 86_400_000));

            authService.login(request);

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken("alice@example.com", "secret")
            );
        }

        @Test
        @DisplayName("propagates BadCredentialsException when authentication fails")
        void propagatesBadCredentials_whenAuthenticationFails() {
            LoginRequest request = new LoginRequest();
            request.setEmail("alice@example.com");
            request.setPassword("wrong");

            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    // -----------------------------------------------------------------------
    // refreshAccessToken()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("refreshAccessToken()")
    class RefreshAccessToken {

        @Test
        @DisplayName("returns new access token for a valid, unexpired refresh token")
        void returnsNewAccessToken_forValidRefreshToken() {
            Jwt refreshJwt = buildJwt(testUser.getId(), 86_400_000);
            String refreshTokenStr = refreshJwt.toString();

            Jwt newAccessJwt = buildJwt(testUser.getId(), 3_600_000);

            when(jwtService.parseToken(refreshTokenStr)).thenReturn(refreshJwt);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn(newAccessJwt);

            Jwt result = authService.refreshAccessToken(refreshTokenStr);

            assertThat(result).isSameAs(newAccessJwt);
        }

        @Test
        @DisplayName("throws BadCredentialsException when parseToken returns null")
        void throwsBadCredentials_whenTokenIsNull() {
            when(jwtService.parseToken("bad-token")).thenReturn(null);

            assertThatThrownBy(() -> authService.refreshAccessToken("bad-token"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("throws BadCredentialsException when refresh token is expired")
        void throwsBadCredentials_whenTokenIsExpired() {
            Jwt expiredJwt = buildJwt(testUser.getId(), -1_000); // already expired
            when(jwtService.parseToken("expired-token")).thenReturn(expiredJwt);

            assertThatThrownBy(() -> authService.refreshAccessToken("expired-token"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("throws NoSuchElementException when user no longer exists in the repository")
        void throwsNoSuchElement_whenUserNoLongerExists() {
            Jwt validJwt = buildJwt(999L, 86_400_000);
            when(jwtService.parseToken("orphaned-token")).thenReturn(validJwt);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshAccessToken("orphaned-token"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }
}
