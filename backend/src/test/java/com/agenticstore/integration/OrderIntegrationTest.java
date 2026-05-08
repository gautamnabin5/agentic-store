package com.agenticstore.integration;

import com.agenticstore.TestcontainersConfig;
import com.agenticstore.entity.Product;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.OrderRepository;
import com.agenticstore.repository.ProductRepository;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class OrderIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    private String customerToken;
    private String customer2Token;
    private String adminToken;
    private Product product;

    @BeforeEach
    void setUp() {
        User customer = userRepository.save(User.builder()
            .email("customer-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Customer One").role(UserRole.CUSTOMER).build());
        customerToken = jwtUtil.generateToken(customer.getId(), customer.getEmail(), "CUSTOMER");

        User customer2 = userRepository.save(User.builder()
            .email("customer2-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Customer Two").role(UserRole.CUSTOMER).build());
        customer2Token = jwtUtil.generateToken(customer2.getId(), customer2.getEmail(), "CUSTOMER");

        User admin = userRepository.save(User.builder()
            .email("admin-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Admin").role(UserRole.ADMIN).build());
        adminToken = jwtUtil.generateToken(admin.getId(), admin.getEmail(), "ADMIN");

        product = productRepository.save(Product.builder()
            .name("Test Widget")
            .price(new BigDecimal("29.99"))
            .stockQuantity(10)
            .build());
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void placeOrder_withSufficientStock_returns201() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 3)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void placeOrder_decrementsStockInDatabase() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 3)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        int remaining = productRepository.findById(product.getId())
            .orElseThrow().getStockQuantity();
        assertEquals(7, remaining);
    }

    @Test
    void placeOrder_returnsItemsInResponse_lazyCollectionLoaded() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 2)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].productName").value("Test Widget"))
            .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void placeOrder_snapshotsUnitPrice() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].unitPrice").value(29.99));
    }

    @Test
    void placeOrder_calculatesCorrectTotal() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 3)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalAmount").value(89.97));
    }

    @Test
    void placeOrder_withInsufficientStock_returns422AndDoesNotDecrementStock() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 100)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnprocessableEntity());

        assertEquals(10, productRepository.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void placeOrder_withoutAuth_returns401() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_withNonExistentProduct_returns404() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", UUID.randomUUID().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());
    }

    @Test
    void placeOrder_withZeroQuantity_returns400() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 0)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listOrders_returnsOnlyOwnOrders() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listOrders_doesNotLeakOtherUsersOrders() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + customer2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listOrders_returnsItemsInEachOrder_lazyLoadingWorks() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].items").isArray())
            .andExpect(jsonPath("$[0].items.length()").value(1));
    }

    @Test
    void getOrder_ownOrder_returns200WithItems() throws Exception {
        var bodyCreate = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 2)));
        var createResp = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(bodyCreate)))
            .andExpect(status().isCreated())
            .andReturn();
        String orderId = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getOrder_otherUsersOrder_returns403() throws Exception {
        var bodyCreate = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        var createResp = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(bodyCreate)))
            .andExpect(status().isCreated())
            .andReturn();
        String orderId = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                .header("Authorization", "Bearer " + customer2Token))
            .andExpect(status().isForbidden());
    }

    @Test
    void getOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminListAllOrders_returns200WithAllOrders() throws Exception {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/orders")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminListAllOrders_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminGetAnyOrder_returns200() throws Exception {
        var bodyCreate = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        var createResp = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(bodyCreate)))
            .andExpect(status().isCreated())
            .andReturn();
        String orderId = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void adminGetAnyOrder_returnsItemsWithLazyLoading() throws Exception {
        var bodyCreate = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 2)));
        var createResp = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(bodyCreate)))
            .andExpect(status().isCreated())
            .andReturn();
        String orderId = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1));
    }
}
