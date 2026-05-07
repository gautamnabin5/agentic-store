package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.security.JwtUtil;
import com.agenticstore.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;
    @MockBean JwtUtil jwtUtil;

    private ProductResponse sampleResponse(UUID id) {
        return new ProductResponse(id, "Shirt", "A shirt",
                new BigDecimal("19.99"), 10, true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listProducts_returns200WithList() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.listActive()).thenReturn(List.of(sampleResponse(id)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Shirt"));
    }

    @Test
    void getProduct_withExistingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(Result.success(sampleResponse(id)));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shirt"));
    }

    @Test
    void getProduct_withMissingId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(Result.failure("Product not found", 404));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void createProduct_withValidBody_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.create(any())).thenReturn(Result.success(sampleResponse(id)));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"Shirt","description":"A shirt","price":19.99,"stockQuantity":10}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Shirt"));
    }

    @Test
    void deleteProduct_withExistingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.softDelete(id)).thenReturn(Result.success(null));

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());
    }
}
