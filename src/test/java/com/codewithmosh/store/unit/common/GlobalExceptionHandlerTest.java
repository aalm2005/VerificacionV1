package com.codewithmosh.store.unit.common;

import com.codewithmosh.store.common.ErrorDto;
import com.codewithmosh.store.common.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Manejador global de errores de la aplicación")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    // -----------------------------------------------------------------------
    // Cuerpo de solicitud ilegible
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el cuerpo de la solicitud no puede leerse o está mal formado")
    class CuerpoIlegible {

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) con el mensaje 'Invalid request body'")
        void retorna400_conMensajeDeErrorDescriptivo() {
            ResponseEntity<ErrorDto> respuesta = manejador.handleUnreadableMessage();

            assertThat(respuesta.getStatusCodeValue()).isEqualTo(400);
            assertThat(respuesta.getBody()).isNotNull();
            assertThat(respuesta.getBody().getError()).isEqualTo("Invalid request body");
        }
    }

    // -----------------------------------------------------------------------
    // Validación fallida
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando uno o más campos del formulario no pasan la validación")
    class ValidacionFallida {

        @Test
        @DisplayName("la respuesta (Código 400) incluye el nombre del campo y su mensaje de error correspondiente")
        void retorna400_conErrorDelCampo_cuandoUnCampoEsInvalido() {
            MethodArgumentNotValidException excepcion = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult               = mock(BindingResult.class);
            when(excepcion.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(
                    List.of(new FieldError("objeto", "email", "El correo electrónico es requerido"))
            );

            ResponseEntity<Map<String, String>> respuesta = manejador.handleException(excepcion);

            assertThat(respuesta.getStatusCodeValue()).isEqualTo(400);
            assertThat(respuesta.getBody())
                    .containsEntry("email", "El correo electrónico es requerido");
        }

        @Test
        @DisplayName("la respuesta (Código 400) incluye TODOS los campos con errores, no solo el primero")
        void retorna400_conTodosLosErrores_cuandoVariosCamposSonInvalidos() {
            MethodArgumentNotValidException excepcion = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult               = mock(BindingResult.class);
            when(excepcion.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(
                    new FieldError("objeto", "email",    "El correo electrónico es requerido"),
                    new FieldError("objeto", "password", "La contraseña debe tener al menos 6 caracteres"),
                    new FieldError("objeto", "name",     "El nombre es requerido")
            ));

            ResponseEntity<Map<String, String>> respuesta = manejador.handleException(excepcion);

            assertThat(respuesta.getStatusCodeValue()).isEqualTo(400);
            assertThat(respuesta.getBody())
                    .containsEntry("email",    "El correo electrónico es requerido")
                    .containsEntry("password", "La contraseña debe tener al menos 6 caracteres")
                    .containsEntry("name",     "El nombre es requerido");
        }
    }
}
