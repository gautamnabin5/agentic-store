package com.agenticstore.dto.product;

import com.agenticstore.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
