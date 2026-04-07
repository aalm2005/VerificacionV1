package com.codewithmosh.store.unit.admin;

import com.codewithmosh.store.admin.AdminController;
import com.codewithmosh.store.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@DisplayName("Controlador del panel de administración (/admin)")
class AdminControllerTest {

    // Reemplaza el SecurityFilterChain de la aplicación con uno de prueba que
    // aplica directamente la regla: /admin/** requiere rol ADMINISTRADOR.
    // Esto evita depender de que SecurityConfig inyecte AdminSecurityRules correctamente.
    @TestConfiguration
    static class ConfiguracionDeSeguridad {

        @Bean
        @Primary
        SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(c -> {
                        c.requestMatchers("/admin/**").hasRole("ADMIN");
                        c.anyRequest().authenticated();
                    })
                    .exceptionHandling(c -> {
                        c.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
                        c.accessDeniedHandler(
                                (req, res, ex) -> res.setStatus(HttpStatus.FORBIDDEN.value()));
                    });
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // -----------------------------------------------------------------------
    // GET /admin/hello
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Verificar acceso al panel de administración (GET /admin/hello)")
    class AccesoAlPanelDeAdministracion {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("la respuesta es exitosa (Código 200) cuando el usuario tiene el rol de administrador")
        void retorna200_cuandoUsuarioEsAdministrador() throws Exception {
            mockMvc.perform(get("/admin/hello"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Hello Admin!"));
        }

        @Test
        @DisplayName("la respuesta indica que el acceso no está autorizado (Código 401) cuando el usuario no inició sesión")
        void retorna401_cuandoUsuarioNoInicioSesion() throws Exception {
            mockMvc.perform(get("/admin/hello"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("la respuesta indica que el acceso está prohibido (Código 403) cuando el usuario inició sesión pero no es administrador")
        void retorna403_cuandoUsuarioNoEsAdministrador() throws Exception {
            mockMvc.perform(get("/admin/hello"))
                    .andExpect(status().isForbidden());
        }
    }
}
