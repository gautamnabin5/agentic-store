package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.entity.Product;
import com.agenticstore.repository.ProductRepository;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @McpTool(
        name = "list_products",
        title = "List Products",
        description = "List all active products available in the store",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, openWorldHint = false)
    )
    public List<ProductResponse> listActive() {
        return productRepository.findAllByActiveTrue().stream()
                .map(ProductResponse::from).toList();
    }

    @McpTool(
        name = "get_product",
        title = "Get Product",
        description = "Get a product by its ID",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false)
    )
    public Result<ProductResponse> getById(
            @McpToolParam(description = "UUID of the product to retrieve") UUID id) {
        return productRepository.findById(id)
                .map(p -> Result.<ProductResponse>ok(ProductResponse.from(p)))
                .orElseGet(() -> Result.failure("Product not found", 404));
    }

    @McpTool(
        name = "create_product",
        title = "Create Product",
        description = "Create a new product in the store",
        annotations = @McpTool.McpAnnotations(destructiveHint = false, openWorldHint = false)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Result<ProductResponse> create(
            @McpToolParam(description = "Product details: name, description, price, and stock quantity") ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();
        return Result.created(ProductResponse.from(productRepository.save(product)));
    }

    @McpTool(
        name = "update_product",
        title = "Update Product",
        description = "Update an existing product's name, description, price, or stock quantity",
        annotations = @McpTool.McpAnnotations(destructiveHint = false, idempotentHint = true, openWorldHint = false)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Result<ProductResponse> update(
            @McpToolParam(description = "UUID of the product to update") UUID id,
            @McpToolParam(description = "Updated product details: name, description, price, and stock quantity") ProductRequest request) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setName(request.name());
                    p.setDescription(request.description());
                    p.setPrice(request.price());
                    p.setStockQuantity(request.stockQuantity());
                    return Result.<ProductResponse>ok(ProductResponse.from(productRepository.save(p)));
                })
                .orElseGet(() -> Result.failure("Product not found", 404));
    }

    @McpTool(
        name = "delete_product",
        title = "Delete Product",
        description = "Soft-delete a product by ID, making it inactive and hidden from the store",
        annotations = @McpTool.McpAnnotations(destructiveHint = true, openWorldHint = false)
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Result<Void> softDelete(
            @McpToolParam(description = "UUID of the product to delete") UUID id) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setActive(false);
                    productRepository.save(p);
                    return Result.<Void>noContent();
                })
                .orElseGet(() -> Result.failure("Product not found", 404));
    }
}
