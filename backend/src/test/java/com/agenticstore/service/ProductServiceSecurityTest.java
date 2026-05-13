package com.agenticstore.service;

import com.agenticstore.dto.product.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceSecurityTest {

    @Autowired
    ProductService productService;

    private ProductRequest sampleRequest() {
        return new ProductRequest("Test", "Desc", BigDecimal.ONE, 10);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> productService.create(sampleRequest()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void update_asCustomer_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class,
            () -> productService.update(UUID.randomUUID(), sampleRequest()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void softDelete_asCustomer_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class,
            () -> productService.softDelete(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_doesNotThrowSecurity() {
        // Security passes; may fail for business reasons (DB not needed here)
        productService.create(sampleRequest());
    }
}
