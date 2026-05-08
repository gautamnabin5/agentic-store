package com.agenticstore.integration;

import com.agenticstore.TestcontainersConfig;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.OrderRepository;
import com.agenticstore.repository.ProductRepository;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class ProductIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
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

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> productBody(String name, double price, int stock) {
        return Map.of("name", name, "price", price, "stockQuantity", stock);
    }

    @Test
    void listProducts_withoutAuth_returns200() {
        ResponseEntity<List> resp = restTemplate.getForEntity("/api/v1/products", List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void listProducts_returnsCreatedProducts() {
        var entity = new HttpEntity<>(productBody("Visible Product", 19.99, 5), bearerHeaders(adminToken));
        restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);

        ResponseEntity<List> resp = restTemplate.getForEntity("/api/v1/products", List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void createProduct_withoutAuth_returns401() {
        var entity = new HttpEntity<>(productBody("Widget", 9.99, 10));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void createProduct_asCustomer_returns403() {
        var entity = new HttpEntity<>(productBody("Widget", 9.99, 10), bearerHeaders(customerToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void createProduct_asAdmin_returns201WithProductData() {
        var entity = new HttpEntity<>(productBody("Gadget Pro", 49.99, 20), bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("id"));
        assertEquals("Gadget Pro", resp.getBody().get("name"));
        assertEquals(20, resp.getBody().get("stockQuantity"));
        assertEquals(true, resp.getBody().get("active"));
    }

    @Test
    void createProduct_withMissingName_returns400() {
        var body = Map.of("price", 9.99, "stockQuantity", 5);
        var entity = new HttpEntity<>(body, bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void createProduct_withNegativeStock_returns400() {
        var body = Map.of("name", "Bad Stock", "price", 9.99, "stockQuantity", -1);
        var entity = new HttpEntity<>(body, bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void getProduct_returnsProductDetails() {
        var entity = new HttpEntity<>(productBody("Detail Product", 19.99, 10), bearerHeaders(adminToken));
        ResponseEntity<Map> createResp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        String id = (String) createResp.getBody().get("id");

        ResponseEntity<Map> getResp = restTemplate.getForEntity("/api/v1/products/" + id, Map.class);
        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        assertEquals("Detail Product", getResp.getBody().get("name"));
    }

    @Test
    void getProduct_withNonExistentId_returns404() {
        ResponseEntity<Map> resp = restTemplate.getForEntity(
            "/api/v1/products/" + UUID.randomUUID(), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void updateProduct_asAdmin_returns200WithUpdatedData() {
        var entity = new HttpEntity<>(productBody("Original Name", 10.00, 5), bearerHeaders(adminToken));
        ResponseEntity<Map> createResp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        String id = (String) createResp.getBody().get("id");

        var updateBody = Map.of("name", "Updated Name", "price", 25.00, "stockQuantity", 15);
        var updateEntity = new HttpEntity<>(updateBody, bearerHeaders(adminToken));
        ResponseEntity<Map> updateResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.PUT, updateEntity, Map.class);

        assertEquals(HttpStatus.OK, updateResp.getStatusCode());
        assertEquals("Updated Name", updateResp.getBody().get("name"));
        assertEquals(15, updateResp.getBody().get("stockQuantity"));
    }

    @Test
    void updateProduct_asCustomer_returns403() {
        var createEntity = new HttpEntity<>(productBody("Product", 10.00, 5), bearerHeaders(adminToken));
        ResponseEntity<Map> createResp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, createEntity, Map.class);
        String id = (String) createResp.getBody().get("id");

        var updateBody = Map.of("name", "Hacked", "price", 1.00, "stockQuantity", 9999);
        var updateEntity = new HttpEntity<>(updateBody, bearerHeaders(customerToken));
        ResponseEntity<Map> updateResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.PUT, updateEntity, Map.class);

        assertEquals(HttpStatus.FORBIDDEN, updateResp.getStatusCode());
    }

    @Test
    void deleteProduct_asAdmin_softDeletesAndReturns204() {
        var entity = new HttpEntity<>(productBody("To Delete", 9.99, 5), bearerHeaders(adminToken));
        ResponseEntity<Map> createResp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        String id = (String) createResp.getBody().get("id");

        var deleteEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.DELETE, deleteEntity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());
        // Soft delete: still in DB but inactive
        var product = productRepository.findById(UUID.fromString(id));
        assertTrue(product.isPresent());
        assertFalse(product.get().isActive());
    }

    @Test
    void deleteProduct_asCustomer_returns403() {
        var entity = new HttpEntity<>(productBody("Protected", 9.99, 5), bearerHeaders(adminToken));
        ResponseEntity<Map> createResp = restTemplate.exchange("/api/v1/products", HttpMethod.POST, entity, Map.class);
        String id = (String) createResp.getBody().get("id");

        var deleteEntity = new HttpEntity<>(bearerHeaders(customerToken));
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.DELETE, deleteEntity, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, deleteResp.getStatusCode());
    }
}
