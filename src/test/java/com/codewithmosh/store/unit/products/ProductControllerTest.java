package com.codewithmosh.store.unit.products;

import com.codewithmosh.store.common.GlobalExceptionHandler;
import com.codewithmosh.store.products.*;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de productos (/products)")
class ProductControllerTest {

    @Mock private ProductRepository  productRepository;
    @Mock private ProductMapper      productMapper;
    @Mock private CategoryRepository categoryRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Product producto;
    private ProductDto productoDto;
    private Category categoria;

    @BeforeEach
    void configurar() {
        var controlador = new ProductController(productRepository, productMapper, categoryRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        categoria   = mock(Category.class);
        producto    = Product.builder().id(1L).name("Auriculares").price(BigDecimal.valueOf(99.99)).build();
        productoDto = new ProductDto();
        productoDto.setId(1L);
        productoDto.setName("Auriculares");
        productoDto.setPrice(BigDecimal.valueOf(99.99));
        productoDto.setCategoryId((byte) 1);
    }

    // -----------------------------------------------------------------------
    // GET /products
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener todos los productos (GET /products)")
    class ObtenerTodosLosProductos {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve todos los productos cuando no se filtra por categoría")
        void retorna200_conTodosLosProductosSinFiltro() throws Exception {
            when(productRepository.findAllWithCategory()).thenReturn(List.of(producto));
            when(productMapper.toDto(producto)).thenReturn(productoDto);

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Auriculares"));
        }

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve solo los productos de la categoría indicada")
        void retorna200_conProductosFiltradosPorCategoria() throws Exception {
            when(productRepository.findAllByCategoryId((byte) 1)).thenReturn(List.of(producto));
            when(productMapper.toDto(producto)).thenReturn(productoDto);

            mockMvc.perform(get("/products").param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Auriculares"));
        }

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) con una lista vacía cuando la categoría no tiene productos")
        void retorna200_conListaVacia_cuandoCategoriaNoTieneProductos() throws Exception {
            when(productRepository.findAllByCategoryId((byte) 5)).thenReturn(List.of());

            mockMvc.perform(get("/products").param("categoryId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // -----------------------------------------------------------------------
    // GET /products/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Obtener un producto por su identificador (GET /products/{id})")
    class ObtenerProductoPorId {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los datos del producto encontrado")
        void retorna200_conDatosDelProducto() throws Exception {
            when(productRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(productMapper.toDto(producto)).thenReturn(productoDto);

            mockMvc.perform(get("/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Auriculares"));
        }

        @Test
        @DisplayName("la respuesta indica que el producto no fue encontrado (Código 404)")
        void retorna404_cuandoProductoNoExiste() throws Exception {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/products/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // -----------------------------------------------------------------------
    // POST /products
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Crear un nuevo producto (POST /products)")
    class CrearProducto {

        @Test
        @DisplayName("la respuesta indica que el recurso fue creado exitosamente (Código 201) con los datos del producto y el encabezado de ubicación")
        void retorna201_conDatosDelProductoYEncabezadoDeUbicacion() throws Exception {
            when(categoryRepository.findById((byte) 1)).thenReturn(Optional.of(categoria));
            when(productMapper.toEntity(any())).thenReturn(producto);
            when(productMapper.toDto(producto)).thenReturn(productoDto);

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productoDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/products/1")))
                    .andExpect(jsonPath("$.name").value("Auriculares"));
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque la categoría indicada no existe")
        void retorna400_cuandoCategoriaNoExiste() throws Exception {
            when(categoryRepository.findById((byte) 1)).thenReturn(Optional.empty());

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productoDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -----------------------------------------------------------------------
    // PUT /products/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Actualizar un producto existente (PUT /products/{id})")
    class ActualizarProducto {

        @Test
        @DisplayName("la respuesta es exitosa (Código 200) y devuelve los datos actualizados del producto")
        void retorna200_conDatosActualizadosDelProducto() throws Exception {
            when(categoryRepository.findById((byte) 1)).thenReturn(Optional.of(categoria));
            when(productRepository.findById(1L)).thenReturn(Optional.of(producto));

            mockMvc.perform(put("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productoDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) porque la categoría indicada no existe")
        void retorna400_cuandoCategoriaNoExiste() throws Exception {
            when(categoryRepository.findById((byte) 1)).thenReturn(Optional.empty());

            mockMvc.perform(put("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productoDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("la respuesta indica que el producto no fue encontrado (Código 404) cuando la categoría existe pero el producto no")
        void retorna404_cuandoProductoNoExiste() throws Exception {
            when(categoryRepository.findById((byte) 1)).thenReturn(Optional.of(categoria));
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            ProductDto dto = new ProductDto();
            dto.setId(99L);
            dto.setCategoryId((byte) 1);

            mockMvc.perform(put("/products/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("la respuesta indica que la solicitud es inválida (Código 400) cuando ni la categoría ni el producto existen — la categoría se verifica primero")
        void retorna400_cuandoNiCategoriaOProductoExisten_verificaCategoriaAntesQueProducto() throws Exception {
            when(categoryRepository.findById((byte) 1)).thenReturn(Optional.empty());

            mockMvc.perform(put("/products/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productoDto)))
                    .andExpect(status().isBadRequest());

            // la verificación del producto no se ejecuta porque la categoría se valida primero
            verify(productRepository, never()).findById(any());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /products/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Eliminar un producto (DELETE /products/{id})")
    class EliminarProducto {

        @Test
        @DisplayName("la respuesta indica que la operación fue exitosa sin contenido (Código 204) cuando el producto existe y fue eliminado")
        void retorna204_cuandoProductoFueEliminado() throws Exception {
            when(productRepository.findById(1L)).thenReturn(Optional.of(producto));

            mockMvc.perform(delete("/products/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("la respuesta indica que el producto no fue encontrado (Código 404)")
        void retorna404_cuandoProductoNoExiste() throws Exception {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/products/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
