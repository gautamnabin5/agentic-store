package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.dto.order.PlaceOrderRequest;
import com.agenticstore.security.UserPrincipal;
import com.agenticstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(principal.id(), request);
    }

    @GetMapping
    public List<OrderResponse> listOwn(@AuthenticationPrincipal UserPrincipal principal) {
        return orderService.listForUser(principal.id());
    }

    @GetMapping("/{id}")
    public Result<OrderResponse> getOwn(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return orderService.getForUser(id, principal.id());
    }
}
