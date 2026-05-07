package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.entity.Product;
import com.agenticstore.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductService productService;

    private Product buildProduct(UUID id, boolean active) {
        return Product.builder()
                .id(id).name("Shirt").description("A shirt")
                .price(new BigDecimal("19.99")).stockQuantity(10)
                .active(active).build();
    }

    @Test
    void listActive_returnsOnlyActiveProducts() {
        Product p = buildProduct(UUID.randomUUID(), true);
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(p));

        List<ProductResponse> results = productService.listActive();

        assertEquals(1, results.size());
        assertEquals("Shirt", results.get(0).name());
    }

    @Test
    void getById_withExistingId_returnsSuccess() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.of(buildProduct(id, true)));

        Result<ProductResponse> result = productService.getById(id);

        assertInstanceOf(Result.Success.class, result);
        assertEquals(id, ((Result.Success<ProductResponse>) result).value().id());
    }

    @Test
    void getById_withMissingId_returnsFailure404() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Result<ProductResponse> result = productService.getById(id);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(404, ((Result.Failure<ProductResponse>) result).httpStatus());
    }

    @Test
    void create_savesAndReturnsProduct() {
        UUID id = UUID.randomUUID();
        Product saved = buildProduct(id, true);
        when(productRepository.save(any())).thenReturn(saved);

        Result<ProductResponse> result = productService.create(
                new ProductRequest("Shirt", "A shirt", new BigDecimal("19.99"), 10));

        assertInstanceOf(Result.Success.class, result);
        assertEquals(id, ((Result.Success<ProductResponse>) result).value().id());
    }

    @Test
    void softDelete_withExistingId_setsActiveFalseAndSaves() {
        UUID id = UUID.randomUUID();
        Product product = buildProduct(id, true);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        Result<Void> result = productService.softDelete(id);

        assertInstanceOf(Result.Success.class, result);
        assertFalse(product.isActive());
        verify(productRepository).save(product);
    }

    @Test
    void softDelete_withMissingId_returnsFailure404() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Result<Void> result = productService.softDelete(id);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(404, ((Result.Failure<Void>) result).httpStatus());
    }
}
