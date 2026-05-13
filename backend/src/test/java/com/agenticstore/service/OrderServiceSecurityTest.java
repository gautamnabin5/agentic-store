package com.agenticstore.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceSecurityTest {

    @Autowired
    OrderService orderService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listAll_asCustomer_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> orderService.listAll());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAll_asAdmin_doesNotThrow() {
        orderService.listAll();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAny_asCustomer_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class,
            () -> orderService.getAny(java.util.UUID.randomUUID()));
    }
}
