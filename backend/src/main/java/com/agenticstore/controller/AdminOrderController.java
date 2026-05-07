package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> listAll() {
        return orderService.listAll();
    }

    @GetMapping("/{id}")
    public Result<OrderResponse> getById(@PathVariable UUID id) {
        return orderService.getAny(id);
    }
}
