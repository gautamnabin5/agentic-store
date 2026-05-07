package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listActive();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return switch (productService.getById(id)) {
            case Result.Success<ProductResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<ProductResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProductRequest request) {
        return switch (productService.create(request)) {
            case Result.Success<ProductResponse> s -> ResponseEntity.status(201).body(s.value());
            case Result.Failure<ProductResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return switch (productService.update(id, request)) {
            case Result.Success<ProductResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<ProductResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable UUID id) {
        return switch (productService.softDelete(id)) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }
}
