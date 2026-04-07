package com.codewithmosh.store.unit.carts;

import com.codewithmosh.store.carts.*;
import com.codewithmosh.store.common.GlobalExceptionHandler;
import com.codewithmosh.store.products.ProductNotFoundException;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de carritos de compra (/carts)")
class CartControllerTest {

    @Mock private CartService cartService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID idCarrito;
    private CartDto carritoDto;
    private CartItemDto itemDto;

    @BeforeEach
    void configurar() {
        var controlador = new CartController(cartService);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        idCarrito  = UUID.randomUUID();
        carritoDto = new CartDto();
        carritoDto.setId(idCarrito);
        carritoDto.setTotalPrice(BigDecimal.ZERO);

        itemDto = new CartItemDto();
        itemDto.setQuantity(1);
        itemDto.setTotalPrice(BigDecimal.TEN);
    }

    // -----------------------------------------------------------------------
    // POST /carts
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Crear un nuevo carrito (POST /carts)")
    class CrearCarrito {

        @Test
        @DisplayName("la respuesta indica que el recurso fue creado exitosamente (Código 201) con los datos del carrito y el encabezado de ubicación")
        void retorna201_conDatosDelNuevoCarritoYEncabezadoDeUbicacion() throws Exception {
            when(cartService.createCart()).thenReturn(carritoDto);

            mockMvc.perform(post("/carts"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.endsWith("/carts/" + idCarrito)))
                    .andExpect(jsonPath("$.id").value(idCarrito.toString()));
        }
    }

    // -----------------------------------------------------------------------
    // POST /carts/{cartId}/items
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Agregar un producto al carrito (POST /carts/{cartId}/items)")
    class AgregarProductoAlCarrito {

        @Test
        @DisplayName("la respuesta indica que el recurso fue creado exitosamente (Código 201) cuando el carrito y el producto existen")
        void retorna201_cuandoCarritoYProductoExisten() throws Exception {
            when(cartService.addToCart(eq(idCarrito), eq(1L))).thenReturn(itemDto);

            AddItemToCartRequest cuerpo = new AddItemToCartRequest();
            cuerpo.setProductId(1L);

            mockMvc.perform(post("/carts/{cartId}/items", idCarrito)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(1));
        }

        @Test
        @DisplayName("la respuesta indica que el recurso no fue encontrado (Código 404) porque el carrito no existe")
        void retorna404_cuandoCarritoNoExiste() throws Exception {
            when(cartService.addToCart(any(), any())).thenThrow(new CartNotFoundException());

            AddItemToCartRequest cuerpo = new AddItemToCartRequest();
            cuerpo.setProductId(1L);

            mockMvc.perform(post("/carts/{cartId}/items", idCarrito)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Cart not found"));
        }

        @Test
        @DisplayName("la respuesta indica que el recurso no fue encontrado (Código 404) porque el producto no existe en el catálogo")
        void retorna404_cuandoProductoNoExiste() throws Exception {
            when(cartService.addToCart(any(), any())).thenThrow(new ProductNotFoundException());

            AddItemToCartRequest cuerpo = new AddItemToCartRequest();
            cuerpo.setProductId(99L);

            mockMvc.perform(post("/carts/{cartId}/items", idCarrito)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Product not found in cart"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque no se proporcionó el identificador del producto")
        void retorna400_cuandoIdDeProductoEsNulo() throws Exception {
            AddItemToCartRequest cuerpo = new AddItemToCartRequest();
            // productId queda nulo intencionalmente

            mockMvc.perform(post("/carts/{cartId}/items", idCarrito)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.productId").exists());
        }
    }

    // -----------------------------------------------------------------------
    // GET /carts/{cartId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Consultar el contenido de un carrito (GET /carts/{cartId})")
    class ConsultarCarrito {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los datos del carrito con sus artículos")
        void retorna200_conDatosDelCarrito() throws Exception {
            when(cartService.getCart(idCarrito)).thenReturn(carritoDto);

            mockMvc.perform(get("/carts/{cartId}", idCarrito))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idCarrito.toString()));
        }

        @Test
        @DisplayName("la respuesta indica que el carrito no fue encontrado (Código 404)")
        void retorna404_cuandoCarritoNoExiste() throws Exception {
            when(cartService.getCart(idCarrito)).thenThrow(new CartNotFoundException());

            mockMvc.perform(get("/carts/{cartId}", idCarrito))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Cart not found"));
        }
    }

    // -----------------------------------------------------------------------
    // PUT /carts/{cartId}/items/{productId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Actualizar la cantidad de un artículo en el carrito (PUT /carts/{cartId}/items/{productId})")
    class ActualizarArticuloDelCarrito {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve el artículo con la cantidad actualizada")
        void retorna200_conArticuloActualizado() throws Exception {
            when(cartService.updateItem(eq(idCarrito), eq(1L), eq(3))).thenReturn(itemDto);

            UpdateCartItemRequest cuerpo = new UpdateCartItemRequest();
            cuerpo.setQuantity(3);

            mockMvc.perform(put("/carts/{cartId}/items/{productId}", idCarrito, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(1));
        }

        @Test
        @DisplayName("la respuesta indica que el carrito no fue encontrado (Código 404)")
        void retorna404_cuandoCarritoNoExiste() throws Exception {
            when(cartService.updateItem(any(), any(), any())).thenThrow(new CartNotFoundException());

            UpdateCartItemRequest cuerpo = new UpdateCartItemRequest();
            cuerpo.setQuantity(3);

            mockMvc.perform(put("/carts/{cartId}/items/{productId}", idCarrito, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Cart not found"));
        }

        @Test
        @DisplayName("la respuesta indica que el artículo no fue encontrado en el carrito (Código 404)")
        void retorna404_cuandoArticuloNoEstaEnElCarrito() throws Exception {
            when(cartService.updateItem(any(), any(), any())).thenThrow(new ProductNotFoundException());

            UpdateCartItemRequest cuerpo = new UpdateCartItemRequest();
            cuerpo.setQuantity(3);

            mockMvc.perform(put("/carts/{cartId}/items/{productId}", idCarrito, 99L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Product not found in cart"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque no se proporcionó la cantidad")
        void retorna400_cuandoCantidadEsNula() throws Exception {
            UpdateCartItemRequest cuerpo = new UpdateCartItemRequest();
            // quantity queda nulo intencionalmente

            mockMvc.perform(put("/carts/{cartId}/items/{productId}", idCarrito, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque la cantidad es cero, que no está permitida")
        void retorna400_cuandoCantidadEsCero() throws Exception {
            UpdateCartItemRequest cuerpo = new UpdateCartItemRequest();
            cuerpo.setQuantity(0);

            mockMvc.perform(put("/carts/{cartId}/items/{productId}", idCarrito, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity").exists());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque la cantidad supera el máximo permitido de 1000")
        void retorna400_cuandoCantidadSuperaElMaximo() throws Exception {
            UpdateCartItemRequest cuerpo = new UpdateCartItemRequest();
            cuerpo.setQuantity(1001);

            mockMvc.perform(put("/carts/{cartId}/items/{productId}", idCarrito, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cuerpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity").exists());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /carts/{cartId}/items/{productId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Eliminar un artículo del carrito (DELETE /carts/{cartId}/items/{productId})")
    class EliminarArticuloDelCarrito {

        @Test
        @DisplayName("la respuesta indica que la operación fue exitosa sin contenido (Código 204) cuando el artículo existe y fue eliminado")
        void retorna204_cuandoArticuloFueEliminado() throws Exception {
            mockMvc.perform(delete("/carts/{cartId}/items/{productId}", idCarrito, 1L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("la respuesta indica que el carrito no fue encontrado (Código 404)")
        void retorna404_cuandoCarritoNoExiste() throws Exception {
            doThrow(new CartNotFoundException()).when(cartService).deleteItem(idCarrito, 1L);

            mockMvc.perform(delete("/carts/{cartId}/items/{productId}", idCarrito, 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Cart not found"));
        }

        @Test
        @DisplayName("la respuesta es exitosa sin contenido (Código 204) cuando el carrito existe pero el artículo no estaba — no es un error eliminar algo inexistente")
        void retorna204_cuandoArticuloNoEstabaEnElCarrito() throws Exception {
            // el servicio simplemente no hace nada si el producto no está en el carrito
            doNothing().when(cartService).deleteItem(idCarrito, 99L);

            mockMvc.perform(delete("/carts/{cartId}/items/{productId}", idCarrito, 99L))
                    .andExpect(status().isNoContent());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /carts/{cartId}/items
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Vaciar todos los artículos de un carrito (DELETE /carts/{cartId}/items)")
    class VaciarCarrito {

        @Test
        @DisplayName("la respuesta indica que la operación fue exitosa sin contenido (Código 204) cuando el carrito existe y fue vaciado")
        void retorna204_cuandoCarritoFueVaciado() throws Exception {
            mockMvc.perform(delete("/carts/{cartId}/items", idCarrito))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("la respuesta indica que el carrito no fue encontrado (Código 404)")
        void retorna404_cuandoCarritoNoExiste() throws Exception {
            doThrow(new CartNotFoundException()).when(cartService).clearCart(idCarrito);

            mockMvc.perform(delete("/carts/{cartId}/items", idCarrito))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Cart not found"));
        }
    }
}
