package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderItemRequest;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.dto.order.PlaceOrderRequest;
import com.agenticstore.entity.*;
import com.agenticstore.repository.OrderRepository;
import com.agenticstore.repository.ProductRepository;
import com.agenticstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    OrderRepository orderRepository = mock(OrderRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    OrderService orderService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, userRepository);
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId).email("user@example.com")
                .name("Alice").role(UserRole.CUSTOMER).build();
    }

    private Product buildProduct(int stock, boolean active) {
        return Product.builder()
                .id(UUID.randomUUID()).name("Shirt")
                .price(new BigDecimal("19.99")).stockQuantity(stock)
                .active(active).build();
    }

    private Order buildSavedOrder(User u, BigDecimal total) {
        return Order.builder()
                .id(UUID.randomUUID()).user(u)
                .totalAmount(total).items(new ArrayList<>()).build();
    }

    @Test
    void placeOrder_withSufficientStock_decrementsStockAndReturnsSuccess() {
        Product product = buildProduct(10, true);
        UUID productId = product.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenReturn(buildSavedOrder(user, new BigDecimal("39.98")));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(productId, 2)));
        Result<OrderResponse> result = orderService.placeOrder(userId, request);

        assertInstanceOf(Result.Success.class, result);
        assertEquals(8, product.getStockQuantity());
        verify(productRepository, never()).save(any());
    }

    @Test
    void placeOrder_withInsufficientStock_returnsFailure422AndDoesNotSaveOrder() {
        Product product = buildProduct(1, true);
        UUID productId = product.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(productId, 5)));
        Result<OrderResponse> result = orderService.placeOrder(userId, request);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(422, ((Result.Failure<OrderResponse>) result).httpStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_withInactiveProduct_returnsFailure404() {
        Product product = buildProduct(10, false);
        UUID productId = product.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(productId, 1)));
        Result<OrderResponse> result = orderService.placeOrder(userId, request);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(404, ((Result.Failure<OrderResponse>) result).httpStatus());
    }

    @Test
    void placeOrder_snapshotsUnitPriceAtOrderTime() {
        Product product = buildProduct(10, true);
        UUID productId = product.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.getItems().forEach(item ->
                    assertEquals(new BigDecimal("19.99"), item.getUnitPrice()));
            return buildSavedOrder(user, new BigDecimal("19.99"));
        });

        orderService.placeOrder(userId, new PlaceOrderRequest(
                List.of(new OrderItemRequest(productId, 1))));
    }

    @Test
    void getForUser_withOtherUsersOrder_returnsFailure403() {
        UUID orderId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Order order = buildSavedOrder(otherUser, BigDecimal.TEN);
        order.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Result<OrderResponse> result = orderService.getForUser(orderId, userId);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(403, ((Result.Failure<OrderResponse>) result).httpStatus());
    }

    @Test
    void getForUser_withOwnOrder_returnsSuccess() {
        UUID orderId = UUID.randomUUID();
        Order order = buildSavedOrder(user, BigDecimal.TEN);
        order.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Result<OrderResponse> result = orderService.getForUser(orderId, userId);

        assertInstanceOf(Result.Success.class, result);
    }
}
