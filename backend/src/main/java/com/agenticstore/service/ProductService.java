package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.entity.Product;
import com.agenticstore.repository.ProductRepository;
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

    public List<ProductResponse> listActive() {
        return productRepository.findAllByActiveTrue().stream()
                .map(ProductResponse::from).toList();
    }

    public Result<ProductResponse> getById(UUID id) {
        return productRepository.findById(id)
                .map(p -> Result.<ProductResponse>success(ProductResponse.from(p)))
                .orElseGet(() -> Result.failure("Product not found", 404));
    }

    @Transactional
    public Result<ProductResponse> create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();
        return Result.success(ProductResponse.from(productRepository.save(product)));
    }

    @Transactional
    public Result<ProductResponse> update(UUID id, ProductRequest request) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setName(request.name());
                    p.setDescription(request.description());
                    p.setPrice(request.price());
                    p.setStockQuantity(request.stockQuantity());
                    return Result.<ProductResponse>success(ProductResponse.from(productRepository.save(p)));
                })
                .orElseGet(() -> Result.failure("Product not found", 404));
    }

    @Transactional
    public Result<Void> softDelete(UUID id) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setActive(false);
                    productRepository.save(p);
                    return Result.<Void>success(null);
                })
                .orElseGet(() -> Result.failure("Product not found", 404));
    }
}
