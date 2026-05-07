package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.security.JwtUtil;
import com.agenticstore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;
    @MockBean JwtUtil jwtUtil;

    private OrderResponse sampleOrder(UUID orderId, UUID userId) {
        return new OrderResponse(orderId, userId, List.of(),
                new BigDecimal("19.99"), LocalDateTime.now());
    }

    @Test
    void listAll_returns200WithAllOrders() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderService.listAll()).thenReturn(List.of(sampleOrder(orderId, userId)));

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }

    @Test
    void getById_withExistingOrder_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(Result.success(sampleOrder(orderId, userId)));

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void getById_withMissingOrder_returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(Result.failure("Order not found", 404));

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }
}
