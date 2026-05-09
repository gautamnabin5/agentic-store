package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.dto.order.PlaceOrderRequest;
import com.agenticstore.entity.Order;
import com.agenticstore.entity.OrderItem;
import com.agenticstore.entity.Product;
import com.agenticstore.repository.OrderRepository;
import com.agenticstore.repository.ProductRepository;
import com.agenticstore.repository.UserRepository;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @McpTool(
        name = "place_order",
        title = "Place Order",
        description = "Place a new order for a user with a list of products and quantities",
        annotations = @McpTool.McpAnnotations(destructiveHint = false, openWorldHint = false)
    )
    @Transactional
    public Result<OrderResponse> placeOrder(
            @McpToolParam(description = "UUID of the user placing the order") UUID userId,
            @McpToolParam(description = "Order details containing a list of product IDs and quantities") PlaceOrderRequest request) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("User not found", 404);
        }

        // Pass 1: validate all items before touching stock
        List<Product> products = new ArrayList<>();
        for (var itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId()).orElse(null);
            if (product == null || !product.isActive()) {
                return Result.failure("Product not found or unavailable", 404);
            }
            if (product.getStockQuantity() < itemReq.quantity()) {
                return Result.failure("Insufficient stock for: " + product.getName(), 422);
            }
            products.add(product);
        }

        // Pass 2: decrement stock and build items
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < request.items().size(); i++) {
            var itemReq = request.items().get(i);
            Product product = products.get(i);

            product.setStockQuantity(product.getStockQuantity() - itemReq.quantity());
            // No explicit save needed — JPA dirty-checking handles this within the transaction

            items.add(OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(product.getPrice())
                    .build());

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .build();
        items.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        return Result.created(OrderResponse.from(orderRepository.save(order)));
    }

    @McpTool(
        name = "list_user_orders",
        title = "List User Orders",
        description = "List all orders placed by a specific user",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, openWorldHint = false)
    )
    @Transactional(readOnly = true)
    public List<OrderResponse> listForUser(
            @McpToolParam(description = "UUID of the user whose orders to retrieve") UUID userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(OrderResponse::from).toList();
    }

    @McpTool(
        name = "get_user_order",
        title = "Get User Order",
        description = "Get a specific order by ID, scoped to the requesting user",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false)
    )
    @Transactional(readOnly = true)
    public Result<OrderResponse> getForUser(
            @McpToolParam(description = "UUID of the order to retrieve") UUID orderId,
            @McpToolParam(description = "UUID of the user who must own the order") UUID userId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    if (!order.getUser().getId().equals(userId)) {
                        return Result.<OrderResponse>failure("Forbidden", 403);
                    }
                    return Result.<OrderResponse>ok(OrderResponse.from(order));
                })
                .orElseGet(() -> Result.failure("Order not found", 404));
    }

    @McpTool(
        name = "list_all_orders",
        title = "List All Orders",
        description = "List all orders across all users (admin use)",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, openWorldHint = false)
    )
    @Transactional(readOnly = true)
    public List<OrderResponse> listAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from).toList();
    }

    @McpTool(
        name = "get_order",
        title = "Get Order",
        description = "Get any order by ID regardless of which user placed it (admin use)",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false)
    )
    @Transactional(readOnly = true)
    public Result<OrderResponse> getAny(
            @McpToolParam(description = "UUID of the order to retrieve") UUID orderId) {
        return orderRepository.findById(orderId)
                .map(o -> Result.<OrderResponse>ok(OrderResponse.from(o)))
                .orElseGet(() -> Result.failure("Order not found", 404));
    }
}
