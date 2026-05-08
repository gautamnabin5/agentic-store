package com.agenticstore.integration;

import com.agenticstore.TestcontainersConfig;
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

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ProductIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        User customer = userRepository.save(User.builder()
            .email("customer-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Customer").role(UserRole.CUSTOMER).build());
        customerToken = jwtUtil.generateToken(customer.getId(), customer.getEmail(), "CUSTOMER");

        User admin = userRepository.save(User.builder()
            .email("admin-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Admin").role(UserRole.ADMIN).build());
        adminToken = jwtUtil.generateToken(admin.getId(), admin.getEmail(), "ADMIN");
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Map<String, Object> productBody(String name, double price, int stock) {
        return Map.of("name", name, "price", price, "stockQuantity", stock);
    }

    @Test
    void listProducts_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk());
    }

    @Test
    void listProducts_returnsCreatedProducts() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("Visible Product", 19.99, 5))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createProduct_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productBody("Widget", 9.99, 10))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(productBody("Widget", 9.99, 10))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_asAdmin_returns201WithProductData() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("Gadget Pro", 49.99, 20))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Gadget Pro"))
            .andExpect(jsonPath("$.stockQuantity").value(20))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createProduct_withMissingName_returns400() throws Exception {
        var body = Map.of("price", 9.99, "stockQuantity", 5);
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_withNegativeStock_returns400() throws Exception {
        var body = Map.of("name", "Bad Stock", "price", 9.99, "stockQuantity", -1);
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getProduct_returnsProductDetails() throws Exception {
        var createResp = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("Detail Product", 19.99, 10))))
            .andExpect(status().isCreated())
            .andReturn();
        String id = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/products/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Detail Product"));
    }

    @Test
    void getProduct_withNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_asAdmin_returns200WithUpdatedData() throws Exception {
        var createResp = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("Original Name", 10.00, 5))))
            .andExpect(status().isCreated())
            .andReturn();
        String id = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        var updateBody = Map.of("name", "Updated Name", "price", 25.00, "stockQuantity", 15);
        mockMvc.perform(put("/api/v1/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.stockQuantity").value(15));
    }

    @Test
    void updateProduct_asCustomer_returns403() throws Exception {
        var createResp = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("Product", 10.00, 5))))
            .andExpect(status().isCreated())
            .andReturn();
        String id = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        var updateBody = Map.of("name", "Hacked", "price", 1.00, "stockQuantity", 9999);
        mockMvc.perform(put("/api/v1/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_asAdmin_softDeletesAndReturns204() throws Exception {
        var createResp = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("To Delete", 9.99, 5))))
            .andExpect(status().isCreated())
            .andReturn();
        String id = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/products/" + id)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        var product = productRepository.findById(UUID.fromString(id));
        assertTrue(product.isPresent());
        assertFalse(product.get().isActive());
    }

    @Test
    void deleteProduct_asCustomer_returns403() throws Exception {
        var createResp = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(productBody("Protected", 9.99, 5))))
            .andExpect(status().isCreated())
            .andReturn();
        String id = objectMapper.readTree(createResp.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/products/" + id)
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
    }
}
