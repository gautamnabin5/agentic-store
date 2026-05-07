package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.security.JwtUtil;
import com.agenticstore.security.UserPrincipal;
import com.agenticstore.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;
    @MockBean JwtUtil jwtUtil;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        var principal = new UserPrincipal(userId, "user@example.com", "CUSTOMER");
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private OrderResponse sampleOrder(UUID orderId) {
        return new OrderResponse(orderId, userId, List.of(),
                new BigDecimal("19.99"), LocalDateTime.now());
    }

    @Test
    void placeOrder_withValidBody_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.placeOrder(eq(userId), any()))
                .thenReturn(Result.created(sampleOrder(orderId)));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"items":[{"productId":"%s","quantity":1}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void placeOrder_withInsufficientStock_returns422() throws Exception {
        when(orderService.placeOrder(eq(userId), any()))
                .thenReturn(Result.failure("Insufficient stock for: Shirt", 422));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"items":[{"productId":"%s","quantity":100}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient stock for: Shirt"));
    }

    @Test
    void listOrders_returns200WithOwnOrders() throws Exception {
        when(orderService.listForUser(userId))
                .thenReturn(List.of(sampleOrder(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void getOrder_withForbiddenOrder_returns403() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getForUser(orderId, userId))
                .thenReturn(Result.failure("Forbidden", 403));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isForbidden());
    }
}
