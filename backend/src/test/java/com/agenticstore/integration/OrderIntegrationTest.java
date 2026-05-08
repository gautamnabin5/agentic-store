package com.agenticstore.integration;

import com.agenticstore.TestcontainersConfig;
import com.agenticstore.entity.Product;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class OrderIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    private User customer;
    private User customer2;
    private User admin;
    private String customerToken;
    private String customer2Token;
    private String adminToken;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(User.builder()
            .email("customer-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Customer One").role(UserRole.CUSTOMER).build());
        customerToken = jwtUtil.generateToken(customer.getId(), customer.getEmail(), "CUSTOMER");

        customer2 = userRepository.save(User.builder()
            .email("customer2-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .name("Customer Two").role(UserRole.CUSTOMER).build());
        customer2Token = jwtUtil.generateToken(customer2.getId(), customer2.getEmail(), "CUSTOMER");

        admin = userRepository.save(User.builder()
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

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<Map> placeOrder(String token, UUID productId, int qty) {
        var body = Map.of("items", List.of(Map.of("productId", productId.toString(), "quantity", qty)));
        return restTemplate.exchange("/api/v1/orders", HttpMethod.POST,
            new HttpEntity<>(body, bearerHeaders(token)), Map.class);
    }

    // ── Place order ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_withSufficientStock_returns201() {
        ResponseEntity<Map> resp = placeOrder(customerToken, product.getId(), 3);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody().get("id"));
    }

    @Test
    void placeOrder_decrementsStockInDatabase() {
        placeOrder(customerToken, product.getId(), 3);

        int remaining = productRepository.findById(product.getId())
            .orElseThrow().getStockQuantity();
        assertEquals(7, remaining);
    }

    @Test
    void placeOrder_returnsItemsInResponse_lazyCollectionLoaded() {
        // Verifies the @Transactional(readOnly=true) fix: OrderResponse.from() accesses
        // order.getItems() which is a lazy @OneToMany — must be within an open session.
        ResponseEntity<Map> resp = placeOrder(customerToken, product.getId(), 2);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        List<?> items = (List<?>) resp.getBody().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        Map<?, ?> item = (Map<?, ?>) items.get(0);
        assertEquals("Test Widget", item.get("productName"));
        assertEquals(2, item.get("quantity"));
    }

    @Test
    void placeOrder_snapshotsUnitPrice() {
        ResponseEntity<Map> resp = placeOrder(customerToken, product.getId(), 1);

        List<?> items = (List<?>) resp.getBody().get("items");
        Map<?, ?> item = (Map<?, ?>) items.get(0);
        double unitPrice = ((Number) item.get("unitPrice")).doubleValue();
        assertEquals(29.99, unitPrice, 0.001);
    }

    @Test
    void placeOrder_calculatesCorrectTotal() {
        ResponseEntity<Map> resp = placeOrder(customerToken, product.getId(), 3);

        // 3 × $29.99 = $89.97
        double total = ((Number) resp.getBody().get("totalAmount")).doubleValue();
        assertEquals(89.97, total, 0.001);
    }

    @Test
    void placeOrder_withInsufficientStock_returns422AndDoesNotDecrementStock() {
        ResponseEntity<Map> resp = placeOrder(customerToken, product.getId(), 100);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals(10, productRepository.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void placeOrder_withoutAuth_returns401() {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 1)));
        var entity = new HttpEntity<>(body);
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/orders", HttpMethod.POST, entity, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void placeOrder_withNonExistentProduct_returns404() {
        ResponseEntity<Map> resp = placeOrder(customerToken, UUID.randomUUID(), 1);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void placeOrder_withZeroQuantity_returns400() {
        var body = Map.of("items", List.of(Map.of("productId", product.getId().toString(), "quantity", 0)));
        var entity = new HttpEntity<>(body, bearerHeaders(customerToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/orders", HttpMethod.POST, entity, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ── List orders ────────────────────────────────────────────────────────────

    @Test
    void listOrders_returnsOnlyOwnOrders() {
        placeOrder(customerToken, product.getId(), 1);

        ResponseEntity<List> resp = restTemplate.exchange("/api/v1/orders", HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customerToken)), List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void listOrders_doesNotLeakOtherUsersOrders() {
        placeOrder(customerToken, product.getId(), 1);

        ResponseEntity<List> resp = restTemplate.exchange("/api/v1/orders", HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customer2Token)), List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().size());
    }

    @Test
    void listOrders_returnsItemsInEachOrder_lazyLoadingWorks() {
        placeOrder(customerToken, product.getId(), 1);

        ResponseEntity<List> resp = restTemplate.exchange("/api/v1/orders", HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customerToken)), List.class);
        List<?> orders = resp.getBody();
        Map<?, ?> order = (Map<?, ?>) orders.get(0);
        List<?> items = (List<?>) order.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
    }

    // ── Get single order ───────────────────────────────────────────────────────

    @Test
    void getOrder_ownOrder_returns200WithItems() {
        ResponseEntity<Map> placed = placeOrder(customerToken, product.getId(), 2);
        String orderId = (String) placed.getBody().get("id");

        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customerToken)), Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<?> items = (List<?>) resp.getBody().get("items");
        assertEquals(1, items.size());
    }

    @Test
    void getOrder_otherUsersOrder_returns403() {
        ResponseEntity<Map> placed = placeOrder(customerToken, product.getId(), 1);
        String orderId = (String) placed.getBody().get("id");

        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customer2Token)), Map.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void getOrder_nonExistent_returns404() {
        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/orders/" + UUID.randomUUID(), HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customerToken)), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ── Admin endpoints ────────────────────────────────────────────────────────

    @Test
    void adminListAllOrders_returns200WithAllOrders() {
        placeOrder(customerToken, product.getId(), 1);

        ResponseEntity<List> resp = restTemplate.exchange("/api/v1/admin/orders", HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(adminToken)), List.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void adminListAllOrders_asCustomer_returns403() {
        ResponseEntity<List> resp = restTemplate.exchange("/api/v1/admin/orders", HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(customerToken)), List.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void adminGetAnyOrder_returns200() {
        ResponseEntity<Map> placed = placeOrder(customerToken, product.getId(), 1);
        String orderId = (String) placed.getBody().get("id");

        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/admin/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(orderId, resp.getBody().get("id"));
    }

    @Test
    void adminGetAnyOrder_returnsItemsWithLazyLoading() {
        ResponseEntity<Map> placed = placeOrder(customerToken, product.getId(), 2);
        String orderId = (String) placed.getBody().get("id");

        ResponseEntity<Map> resp = restTemplate.exchange("/api/v1/admin/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(adminToken)), Map.class);

        List<?> items = (List<?>) resp.getBody().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
    }
}
