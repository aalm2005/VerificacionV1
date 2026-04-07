package com.codewithmosh.store.unit.orders;

import com.codewithmosh.store.common.GlobalExceptionHandler;
import com.codewithmosh.store.orders.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de pedidos (/orders)")
class OrderControllerTest {

    @Mock private OrderService orderService;

    private MockMvc mockMvc;

    private OrderDto pedidoDto;

    @BeforeEach
    void configurar() {
        var controlador = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        pedidoDto = new OrderDto();
        pedidoDto.setId(10L);
        pedidoDto.setStatus("PENDING");
        pedidoDto.setTotalPrice(BigDecimal.valueOf(100));
    }

    // -----------------------------------------------------------------------
    // GET /orders
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener todos los pedidos del usuario autenticado (GET /orders)")
    class ObtenerTodosLosPedidos {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve la lista de pedidos del usuario")
        void retorna200_conListaDePedidosDelUsuario() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of(pedidoDto));

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(10L))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) con una lista vacía cuando el usuario no tiene pedidos")
        void retorna200_conListaVacia_cuandoUsuarioNoTienePedidos() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of());

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // GET /orders/{orderId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener el detalle de un pedido específico (GET /orders/{orderId})")
    class ObtenerDetalleDePedido {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los detalles del pedido cuando pertenece al usuario")
        void retorna200_conDetallesDelPedidoCuandoPerteneceAlUsuario() throws Exception {
            when(orderService.getOneOrder(10L)).thenReturn(pedidoDto);

            mockMvc.perform(get("/orders/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalPrice").value(100));
        }

        @Test
        @DisplayName("la respuesta indica que el pedido no fue encontrado (Código 404)")
        void retorna404_cuandoPedidoNoExiste() throws Exception {
            when(orderService.getOneOrder(99L)).thenThrow(new OrderNotFoundException());

            mockMvc.perform(get("/orders/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("la respuesta indica que el acceso al pedido está prohibido (Código 403) porque pertenece a otro usuario")
        void retorna403_cuandoPedidoPerteneceAOtroUsuario() throws Exception {
            when(orderService.getOneOrder(10L))
                    .thenThrow(new AccessDeniedException("No tienes acceso a este pedido"));

            mockMvc.perform(get("/orders/10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("No tienes acceso a este pedido"));
        }
    }
}
