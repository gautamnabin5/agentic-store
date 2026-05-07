package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public Result<ProductResponse> getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @PostMapping
    public Result<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public Result<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable UUID id) {
        return productService.softDelete(id);
    }
}
