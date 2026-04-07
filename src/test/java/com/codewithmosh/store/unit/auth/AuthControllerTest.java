package com.codewithmosh.store.unit.auth;

import com.codewithmosh.store.auth.*;
import com.codewithmosh.store.common.GlobalExceptionHandler;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import com.codewithmosh.store.users.UserDto;
import com.codewithmosh.store.users.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de autenticación (/auth)")
class AuthControllerTest {

    @Mock private AuthService  authService;
    @Mock private JwtConfig    jwtConfig;
    @Mock private UserMapper   userMapper;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final SecretKey CLAVE_PRUEBA =
            Keys.hmacShaKeyFor("clave-secreta-de-prueba-suficientemente-larga!!".getBytes());

    @BeforeEach
    void configurar() {
        var controlador = new AuthController(userMapper, jwtConfig, authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
    // POST /auth/login
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Iniciar sesión (POST /auth/login)")
    class IniciarSesion {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200), devuelve el token de acceso y guarda el token de renovación en una cookie segura")
        void retorna200_conTokenDeAccesoYCookieSegura() throws Exception {
            Jwt tokenAcceso    = construirToken(1L, Role.USER, 3_600_000);
            Jwt tokenRenovacion = construirToken(1L, Role.USER, 604_800_000);

            when(authService.login(any())).thenReturn(new LoginResponse(tokenAcceso, tokenRenovacion));
            when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800);

            LoginRequest cuerpo = new LoginRequest();
            cuerpo.setEmail("alice@example.com");
            cuerpo.setPassword("secreto123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().path("refreshToken", "/auth/refresh"))
                    .andExpect(cookie().secure("refreshToken", true))
                    .andExpect(cookie().maxAge("refreshToken", 604800));
        }

        @Test
        @DisplayName("la respuesta indica que las credenciales son incorrectas (Código 401) cuando el correo o la contraseña son incorrectos")
        void retorna401_cuandoCredencialesSonIncorrectas() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Credenciales inválidas"));

            LoginRequest cuerpo = new LoginRequest();
            cuerpo.setEmail("alice@example.com");
            cuerpo.setPassword("contraseña-incorrecta");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) cuando el correo está vacío")
        void retorna400_cuandoCorreoEstaVacio() throws Exception {
            LoginRequest cuerpo = new LoginRequest();
            cuerpo.setEmail("");
            cuerpo.setPassword("secreto123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) cuando el correo no tiene el formato correcto")
        void retorna400_cuandoCorreoNoTieneFormatoCorrecto() throws Exception {
            LoginRequest cuerpo = new LoginRequest();
            cuerpo.setEmail("esto-no-es-un-correo");
            cuerpo.setPassword("secreto123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) cuando la contraseña está vacía")
        void retorna400_cuandoContraseñaEstaVacia() throws Exception {
            LoginRequest cuerpo = new LoginRequest();
            cuerpo.setEmail("alice@example.com");
            cuerpo.setPassword("");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }
    }

    // -----------------------------------------------------------------------
    // POST /auth/refresh
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Renovar el token de acceso (POST /auth/refresh)")
    class RenovarTokenDeAcceso {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve un nuevo token de acceso válido")
        void retorna200_conNuevoTokenDeAcceso() throws Exception {
            Jwt nuevoToken = construirToken(1L, Role.USER, 3_600_000);
            when(authService.refreshAccessToken("token-renovacion-valido")).thenReturn(nuevoToken);

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", "token-renovacion-valido")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("la respuesta indica que el token de renovación es inválido o ya venció (Código 401)")
        void retorna401_cuandoTokenDeRenovacionEsInvalidoOVencio() throws Exception {
            when(authService.refreshAccessToken("token-invalido"))
                    .thenThrow(new BadCredentialsException("Token de renovación inválido"));

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", "token-invalido")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) cuando no se envía la cookie del token de renovación")
        void retorna400_cuandoNoCookieEnviada() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isBadRequest());
        }
    }

    // -----------------------------------------------------------------------
    // GET /auth/me
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener el usuario que inició sesión (GET /auth/me)")
    class ObtenerUsuarioActual {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los datos del usuario autenticado")
        void retorna200_conDatosDelUsuarioAutenticado() throws Exception {
            User usuario = User.builder().id(1L).name("Alice").email("alice@example.com").build();
            UserDto dto  = new UserDto(1L, "Alice", "alice@example.com");

            when(authService.getCurrentUser()).thenReturn(usuario);
            when(userMapper.toDto(usuario)).thenReturn(dto);

            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("la respuesta indica que el recurso no fue encontrado (Código 404) cuando el usuario no existe en el sistema")
        void retorna404_cuandoUsuarioNoExisteEnElSistema() throws Exception {
            when(authService.getCurrentUser()).thenReturn(null);

            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isNotFound());
        }
    }
}
