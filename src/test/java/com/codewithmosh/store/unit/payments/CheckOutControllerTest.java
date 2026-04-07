package com.codewithmosh.store.unit.payments;

import com.codewithmosh.store.carts.CartEmptyException;
import com.codewithmosh.store.carts.CartNotFoundException;
import com.codewithmosh.store.common.GlobalExceptionHandler;
import com.codewithmosh.store.orders.OrderRepository;
import com.codewithmosh.store.payments.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de pagos y proceso de compra (/checkout)")
class CheckOutControllerTest {

    @Mock private CheckoutService  checkoutService;
    @Mock private OrderRepository  orderRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void configurar() {
        var controlador = new CheckOutController(checkoutService, orderRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /checkout
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Iniciar el proceso de pago (POST /checkout)")
    class IniciarPago {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) con el identificador del pedido y la URL de pago cuando el carrito tiene artículos")
        void retorna200_conIdPedidoYUrlDePago() throws Exception {
            CheckOutResponse respuestaPago = new CheckOutResponse(42L, "https://pago.stripe.com/sesion123");
            when(checkoutService.checkout(any())).thenReturn(respuestaPago);

            CheckOutRequest cuerpo = new CheckOutRequest();
            cuerpo.setCartId(UUID.randomUUID());

            mockMvc.perform(post("/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(42L))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://pago.stripe.com/sesion123"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el carrito no existe")
        void retorna400_cuandoCarritoNoExiste() throws Exception {
            when(checkoutService.checkout(any()))
                    .thenThrow(new CartNotFoundException());

            CheckOutRequest cuerpo = new CheckOutRequest();
            cuerpo.setCartId(UUID.randomUUID());

            mockMvc.perform(post("/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque el carrito está vacío")
        void retorna400_cuandoCarritoEstaVacio() throws Exception {
            when(checkoutService.checkout(any()))
                    .thenThrow(new CartEmptyException());

            CheckOutRequest cuerpo = new CheckOutRequest();
            cuerpo.setCartId(UUID.randomUUID());

            mockMvc.perform(post("/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("la respuesta indica un error interno del servidor (Código 500) cuando el servicio de pago falla")
        void retorna500_cuandoServicioDePagoFalla() throws Exception {
            when(checkoutService.checkout(any()))
                    .thenThrow(new PaymentException("Error al comunicarse con el servicio de pago"));

            CheckOutRequest cuerpo = new CheckOutRequest();
            cuerpo.setCartId(UUID.randomUUID());

            mockMvc.perform(post("/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Error creating a checkout session"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque no se proporcionó el identificador del carrito")
        void retorna400_cuandoIdDelCarritoEsNulo() throws Exception {
            CheckOutRequest cuerpo = new CheckOutRequest();
            // cartId queda nulo intencionalmente

            mockMvc.perform(post("/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.cartId").exists());
        }
    }

    // -----------------------------------------------------------------------
    // POST /checkout/webhook
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Recibir notificación de pago del proveedor (POST /checkout/webhook)")
    class RecibirNotificacionDePago {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) cuando la notificación se procesa correctamente")
        void retorna200_cuandoNotificacionSeProcesaCorrectamente() throws Exception {
            mockMvc.perform(post("/checkout/webhook")
                            .header("stripe-signature", "firma-de-prueba")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("{\"type\":\"payment_intent.succeeded\"}"))
                    .andExpect(status().isOk());
        }
    }
}
