package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.dto.order.PlaceOrderRequest;
import com.agenticstore.security.UserPrincipal;
import com.agenticstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        return switch (orderService.placeOrder(principal.id(), request)) {
            case Result.Success<OrderResponse> s -> ResponseEntity.status(201).body(s.value());
            case Result.Failure<OrderResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @GetMapping
    public List<OrderResponse> listOwn(@AuthenticationPrincipal UserPrincipal principal) {
        return orderService.listForUser(principal.id());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOwn(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return switch (orderService.getForUser(id, principal.id())) {
            case Result.Success<OrderResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<OrderResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }
}
