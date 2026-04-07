package com.codewithmosh.store.unit.users;

import com.codewithmosh.store.common.GlobalExceptionHandler;
import com.codewithmosh.store.users.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de usuarios (/users)")
class UserControllerTest {

    @Mock private UserRepository  userRepository;
    @Mock private UserMapper      userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserService     userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void configurar() {
        var controlador = new UserController(userRepository, userMapper, passwordEncoder, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // GET /users
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener todos los usuarios (GET /users)")
    class ObtenerTodosLosUsuarios {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve la lista de usuarios cuando no se especifica orden")
        void retorna200_conListaDeUsuariosSinOrden() throws Exception {
            when(userService.getAllUsers("")).thenReturn(
                    List.of(new UserDto(1L, "Alice", "alice@example.com"))
            );

            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Alice"))
                    .andExpect(jsonPath("$[0].email").value("alice@example.com"));
        }

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los usuarios cuando se ordena por correo electrónico")
        void retorna200_conUsuariosOrdenadosPorCorreo() throws Exception {
            when(userService.getAllUsers("email")).thenReturn(
                    List.of(new UserDto(1L, "Alice", "alice@example.com"))
            );

            mockMvc.perform(get("/users").param("sort", "email"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("alice@example.com"));
        }
    }

    // -----------------------------------------------------------------------
    // GET /users/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener un usuario por su identificador (GET /users/{id})")
    class ObtenerUsuarioPorId {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los datos del usuario encontrado")
        void retorna200_conDatosDelUsuario() throws Exception {
            when(userService.getUser(1L)).thenReturn(new UserDto(1L, "Alice", "alice@example.com"));

            mockMvc.perform(get("/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("la respuesta indica que el usuario no fue encontrado (Código 404)")
        void retorna404_cuandoUsuarioNoExiste() throws Exception {
            when(userService.getUser(99L)).thenThrow(new UserNotFoundException());

            mockMvc.perform(get("/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    // -----------------------------------------------------------------------
    // POST /users
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Registrar un nuevo usuario (POST /users)")
    class RegistrarUsuario {

        private RegisterUserRequest construirSolicitudValida() {
            RegisterUserRequest req = new RegisterUserRequest();
            req.setName("Bob");
            req.setEmail("bob@example.com");
            req.setPassword("secreto123");
            return req;
        }

        @Test
        @DisplayName("la respuesta indica que el recurso fue creado exitosamente (Código 201) con los datos del nuevo usuario y el encabezado de ubicación")
        void retorna201_conDatosDelNuevoUsuarioYEncabezadoDeUbicacion() throws Exception {
            when(userService.registerUser(any())).thenReturn(new UserDto(2L, "Bob", "bob@example.com"));

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(construirSolicitudValida())))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/users/2")))
                    .andExpect(jsonPath("$.id").value(2L))
                    .andExpect(jsonPath("$.name").value("Bob"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el correo ya está registrado en el sistema")
        void retorna400_cuandoCorreoYaEstaRegistrado() throws Exception {
            when(userService.registerUser(any())).thenThrow(new DuplicateUserException());

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(construirSolicitudValida())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Email is already registered"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el nombre está vacío")
        void retorna400_cuandoNombreEstaVacio() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setName("");

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.name").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el nombre excede los 255 caracteres permitidos")
        void retorna400_cuandoNombreExcedeLongitudMaxima() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setName("A".repeat(256));

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.name").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el correo está vacío")
        void retorna400_cuandoCorreoEstaVacio() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setEmail("");

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el correo no tiene el formato correcto")
        void retorna400_cuandoCorreoNoTieneFormatoCorrecto() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setEmail("esto-no-es-un-correo");

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el correo contiene letras mayúsculas")
        void retorna400_cuandoCorreoTieneLetrasEnMayusculas() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setEmail("Alice@example.com");

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque la contraseña tiene menos de 6 caracteres")
        void retorna400_cuandoContraseñaTieneMenosDe6Caracteres() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setPassword("corta");

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque la contraseña excede los 25 caracteres permitidos")
        void retorna400_cuandoContraseñaExcede25Caracteres() throws Exception {
            RegisterUserRequest req = construirSolicitudValida();
            req.setPassword("contraseña-demasiado-larga-123");

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }
    }

    // -----------------------------------------------------------------------
    // PUT /users/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Actualizar los datos de un usuario (PUT /users/{id})")
    class ActualizarUsuario {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los datos actualizados del usuario")
        void retorna200_conDatosActualizadosDelUsuario() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setName("Alice Actualizada");
            req.setEmail("alice.nueva@example.com");

            when(userService.updateUser(eq(1L), any()))
                    .thenReturn(new UserDto(1L, "Alice Actualizada", "alice.nueva@example.com"));

            mockMvc.perform(put("/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Alice Actualizada"))
                    .andExpect(jsonPath("$.email").value("alice.nueva@example.com"));
        }

        @Test
        @DisplayName("la respuesta indica que el usuario no fue encontrado (Código 404)")
        void retorna404_cuandoUsuarioNoExiste() throws Exception {
            when(userService.updateUser(eq(99L), any())).thenThrow(new UserNotFoundException());

            mockMvc.perform(put("/users/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }

        @Test
        @DisplayName("la respuesta indica que el acceso no está permitido (Código 401) cuando el usuario intenta modificar la cuenta de otra persona")
        void retorna401_cuandoAccesoEstaProhibido() throws Exception {
            when(userService.updateUser(eq(2L), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(put("/users/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Access denied"));
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /users/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Eliminar un usuario (DELETE /users/{id})")
    class EliminarUsuario {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) cuando el usuario existe y fue eliminado correctamente")
        void retorna200_cuandoUsuarioFueEliminado() throws Exception {
            mockMvc.perform(delete("/users/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("la respuesta indica que el usuario no fue encontrado (Código 404)")
        void retorna404_cuandoUsuarioNoExiste() throws Exception {
            doThrow(new UserNotFoundException()).when(userService).deleteUser(99L);

            mockMvc.perform(delete("/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    // -----------------------------------------------------------------------
    // POST /users/{id}/change-password
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cambiar la contraseña de un usuario (POST /users/{id}/change-password)")
    class CambiarContraseña {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) cuando la contraseña anterior es correcta")
        void retorna200_cuandoContraseñaAnteriorEsCorrecta() throws Exception {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("contraseña-vieja");
            req.setNewPassword("contraseña-nueva");

            mockMvc.perform(post("/users/1/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("la respuesta indica que el acceso no está permitido (Código 401) cuando la contraseña anterior es incorrecta")
        void retorna401_cuandoContraseñaAnteriorEsIncorrecta() throws Exception {
            doThrow(new AccessDeniedException("Contraseña incorrecta"))
                    .when(userService).changePassword(eq(1L), any());

            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("contraseña-incorrecta");
            req.setNewPassword("contraseña-nueva");

            mockMvc.perform(post("/users/1/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Access denied"));
        }

        @Test
        @DisplayName("la respuesta indica que el usuario no fue encontrado (Código 404)")
        void retorna404_cuandoUsuarioNoExiste() throws Exception {
            doThrow(new UserNotFoundException()).when(userService).changePassword(eq(99L), any());

            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("contraseña-vieja");
            req.setNewPassword("contraseña-nueva");

            mockMvc.perform(post("/users/99/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }
}
