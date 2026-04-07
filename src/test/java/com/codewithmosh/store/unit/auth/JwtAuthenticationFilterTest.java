package com.codewithmosh.store.unit.auth;

import com.codewithmosh.store.auth.Jwt;
import com.codewithmosh.store.auth.JwtAuthenticationFilter;
import com.codewithmosh.store.auth.JwtService;
import com.codewithmosh.store.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Filtro de autenticación con token JWT")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filtro;

    private static final SecretKey CLAVE_PRUEBA =
            Keys.hmacShaKeyFor("clave-secreta-de-prueba-suficientemente-larga!!".getBytes());

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private Jwt construirToken(Long userId, Role rol, long vencimientoMs) {
        Claims claims = Jwts.claims()
                .subject(userId.toString())
                .add("role", rol.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + vencimientoMs))
                .build();
        return new Jwt(claims, CLAVE_PRUEBA);
    }

    // -----------------------------------------------------------------------
    // Sin encabezado de autorización
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando la solicitud llega sin encabezado de autorización")
    class SinEncabezadoAutorizacion {

        @Test
        @DisplayName("deja pasar la solicitud sin autenticar al usuario")
        void dejaPasarSolicitud_sinAutenticar() throws Exception {
            var solicitud = new MockHttpServletRequest();
            var respuesta = new MockHttpServletResponse();

            filtro.doFilter(solicitud, respuesta, filterChain);

            verify(filterChain).doFilter(solicitud, respuesta);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // Encabezado con formato incorrecto
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el encabezado de autorización no usa el formato 'Bearer <token>'")
    class EncabezadoConFormatoIncorrecto {

        @Test
        @DisplayName("deja pasar la solicitud sin autenticar al usuario")
        void dejaPasarSolicitud_cuandoFormatoEsIncorrecto() throws Exception {
            var solicitud = new MockHttpServletRequest();
            solicitud.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            var respuesta = new MockHttpServletResponse();

            filtro.doFilter(solicitud, respuesta, filterChain);

            verify(filterChain).doFilter(solicitud, respuesta);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // Token inválido
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el token JWT no puede interpretarse porque está malformado o es inválido")
    class TokenInvalido {

        @Test
        @DisplayName("deja pasar la solicitud sin autenticar al usuario")
        void dejaPasarSolicitud_cuandoTokenEsInvalido() throws Exception {
            var solicitud = new MockHttpServletRequest();
            solicitud.addHeader("Authorization", "Bearer token.malformado.xyz");
            var respuesta = new MockHttpServletResponse();

            when(jwtService.parseToken("token.malformado.xyz")).thenReturn(null);

            filtro.doFilter(solicitud, respuesta, filterChain);

            verify(filterChain).doFilter(solicitud, respuesta);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // Token vencido
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el token JWT ya venció")
    class TokenVencido {

        @Test
        @DisplayName("deja pasar la solicitud sin autenticar al usuario")
        void dejaPasarSolicitud_cuandoTokenEstaVencido() throws Exception {
            Jwt tokenVencido = construirToken(1L, Role.USER, -60_000); // venció hace 1 minuto
            var solicitud    = new MockHttpServletRequest();
            solicitud.addHeader("Authorization", "Bearer token-vencido");
            var respuesta = new MockHttpServletResponse();

            when(jwtService.parseToken("token-vencido")).thenReturn(tokenVencido);

            filtro.doFilter(solicitud, respuesta, filterChain);

            verify(filterChain).doFilter(solicitud, respuesta);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // Token válido
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el token JWT es válido y está vigente")
    class TokenValido {

        @Test
        @DisplayName("autentica al usuario con rol USUARIO y lo registra en el contexto de seguridad")
        void autenticaUsuario_conRolUsuario() throws Exception {
            Jwt tokenValido = construirToken(1L, Role.USER, 3_600_000);
            var solicitud   = new MockHttpServletRequest();
            solicitud.addHeader("Authorization", "Bearer token-usuario-valido");
            var respuesta = new MockHttpServletResponse();

            when(jwtService.parseToken("token-usuario-valido")).thenReturn(tokenValido);

            filtro.doFilter(solicitud, respuesta, filterChain);

            verify(filterChain).doFilter(solicitud, respuesta);
            var autenticacion = SecurityContextHolder.getContext().getAuthentication();
            assertThat(autenticacion).isNotNull();
            assertThat(autenticacion.getPrincipal()).isEqualTo(1L);
            assertThat(autenticacion.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("autentica al usuario con rol ADMINISTRADOR y lo registra en el contexto de seguridad")
        void autenticaUsuario_conRolAdministrador() throws Exception {
            Jwt tokenAdmin = construirToken(2L, Role.ADMIN, 3_600_000);
            var solicitud  = new MockHttpServletRequest();
            solicitud.addHeader("Authorization", "Bearer token-admin-valido");
            var respuesta  = new MockHttpServletResponse();

            when(jwtService.parseToken("token-admin-valido")).thenReturn(tokenAdmin);

            filtro.doFilter(solicitud, respuesta, filterChain);

            var autenticacion = SecurityContextHolder.getContext().getAuthentication();
            assertThat(autenticacion).isNotNull();
            assertThat(autenticacion.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }
}
