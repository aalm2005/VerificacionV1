package com.codewithmosh.store.unit.users;

import com.codewithmosh.store.users.User;
import com.codewithmosh.store.users.UserDetailsServiceImpl;
import com.codewithmosh.store.users.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de carga de datos de usuario por correo electrónico")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl servicio;

    // -----------------------------------------------------------------------
    // Correo existente
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el correo electrónico corresponde a un usuario registrado")
    class CorreoExistente {

        @Test
        @DisplayName("devuelve los datos del usuario con su correo, contraseña cifrada y sin permisos adicionales")
        void devuelveDatosDeUsuario_cuandoCorreoExiste() {
            User usuario = User.builder()
                    .id(1L)
                    .email("alice@example.com")
                    .password("contraseña-cifrada-bcrypt")
                    .build();

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(usuario));

            UserDetails resultado = servicio.loadUserByUsername("alice@example.com");

            assertThat(resultado.getUsername()).isEqualTo("alice@example.com");
            assertThat(resultado.getPassword()).isEqualTo("contraseña-cifrada-bcrypt");
            assertThat(resultado.getAuthorities()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // Correo no existente
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el correo electrónico no está registrado en el sistema")
    class CorreoNoExistente {

        @Test
        @DisplayName("lanza un error indicando que el usuario no fue encontrado")
        void lanzaError_cuandoCorreoNoExiste() {
            when(userRepository.findByEmail("inexistente@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.loadUserByUsername("inexistente@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");
        }
    }
}
