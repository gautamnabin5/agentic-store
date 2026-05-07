# Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Spring Boot REST API for a simple online store with users, products, and orders backed by PostgreSQL.

**Architecture:** Standard Spring Boot 3 layered architecture (controller → service → repository). Services return `Result<T>` instead of throwing exceptions. `OrderService.placeOrder()` runs in a single `@Transactional` block. Schema is managed exclusively through Flyway migrations.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Security + JWT (jjwt 0.12), Spring Data JPA, PostgreSQL 16, Flyway, Lombok, JUnit 5, Mockito, Testcontainers

---

## File Map

```
backend/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/java/com/agenticstore/
    │   ├── AgenticStoreApplication.java
    │   ├── common/
    │   │   └── Result.java
    │   ├── entity/
    │   │   ├── UserRole.java
    │   │   ├── User.java
    │   │   ├── Product.java
    │   │   ├── Order.java
    │   │   └── OrderItem.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── ProductRepository.java
    │   │   ├── OrderRepository.java
    │   │   └── OrderItemRepository.java
    │   ├── dto/
    │   │   ├── auth/RegisterRequest.java
    │   │   ├── auth/LoginRequest.java
    │   │   ├── auth/AuthResponse.java
    │   │   ├── product/ProductRequest.java
    │   │   ├── product/ProductResponse.java
    │   │   ├── order/OrderItemRequest.java
    │   │   ├── order/PlaceOrderRequest.java
    │   │   ├── order/OrderItemResponse.java
    │   │   └── order/OrderResponse.java
    │   ├── service/
    │   │   ├── AuthService.java
    │   │   ├── ProductService.java
    │   │   └── OrderService.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── ProductController.java
    │   │   ├── OrderController.java
    │   │   └── AdminOrderController.java
    │   └── security/
    │       ├── JwtUtil.java
    │       ├── JwtAuthFilter.java
    │       ├── SecurityConfig.java
    │       └── UserPrincipal.java
    ├── main/resources/
    │   ├── application.properties
    │   └── db/migration/
    │       ├── V1__create_users.sql
    │       ├── V2__create_products.sql
    │       ├── V3__create_orders.sql
    │       └── V4__create_order_items.sql
    └── test/java/com/agenticstore/
        ├── AgenticStoreApplicationTests.java
        ├── TestcontainersConfig.java
        ├── common/ResultTest.java
        ├── service/AuthServiceTest.java
        ├── service/ProductServiceTest.java
        ├── service/OrderServiceTest.java
        ├── controller/AuthControllerTest.java
        ├── controller/ProductControllerTest.java
        └── controller/OrderControllerTest.java
```

---

## Task 1: Spring Boot project scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/agenticstore/AgenticStoreApplication.java`
- Create: `backend/src/main/resources/application.properties`

- [ ] **Step 1: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>
    <groupId>com.agenticstore</groupId>
    <artifactId>agentic-store</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>agentic-store</name>
    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <testcontainers.version>1.19.8</testcontainers.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create `backend/src/main/java/com/agenticstore/AgenticStoreApplication.java`**

```java
package com.agenticstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgenticStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgenticStoreApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `backend/src/main/resources/application.properties`**

```properties
spring.application.name=agentic-store

spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:agenticstore}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

app.jwt.secret=${JWT_SECRET:change-me-in-production-this-is-a-very-long-secret-key-for-dev-only}
app.jwt.expiration-ms=86400000
```

- [ ] **Step 4: Download dependencies**

Run from `backend/`:
```bash
./mvnw dependency:go-offline -q
```

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat: scaffold Spring Boot project with dependencies"
```

---

## Task 2: Result\<T\> type

**Files:**
- Create: `backend/src/main/java/com/agenticstore/common/Result.java`
- Create: `backend/src/test/java/com/agenticstore/common/ResultTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/common/ResultTest.java`:

```java
package com.agenticstore.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_wrapsValue() {
        Result<String> result = Result.success("hello");
        assertInstanceOf(Result.Success.class, result);
        assertEquals("hello", ((Result.Success<String>) result).value());
    }

    @Test
    void failure_wrapsErrorAndStatus() {
        Result<String> result = Result.failure("Not found", 404);
        assertInstanceOf(Result.Failure.class, result);
        assertEquals("Not found", ((Result.Failure<String>) result).error());
        assertEquals(404, ((Result.Failure<String>) result).httpStatus());
    }

    @Test
    void success_and_failure_areDistinctTypes() {
        Result<String> ok = Result.success("ok");
        Result<String> err = Result.failure("err", 500);
        assertInstanceOf(Result.Success.class, ok);
        assertInstanceOf(Result.Failure.class, err);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -pl . -Dtest=ResultTest -q 2>&1 | tail -5
```

Expected: compilation error — `Result` does not exist.

- [ ] **Step 3: Create `backend/src/main/java/com/agenticstore/common/Result.java`**

```java
package com.agenticstore.common;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {}

    record Failure<T>(String error, int httpStatus) implements Result<T> {}

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String error, int httpStatus) {
        return new Failure<>(error, httpStatus);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=ResultTest -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -m "feat: add Result<T> sealed type for service return values"
```

---

## Task 3: Flyway migrations and context load test

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_users.sql`
- Create: `backend/src/main/resources/db/migration/V2__create_products.sql`
- Create: `backend/src/main/resources/db/migration/V3__create_orders.sql`
- Create: `backend/src/main/resources/db/migration/V4__create_order_items.sql`
- Create: `backend/src/test/java/com/agenticstore/TestcontainersConfig.java`
- Create: `backend/src/test/java/com/agenticstore/AgenticStoreApplicationTests.java`

- [ ] **Step 1: Write the failing context test**

Create `backend/src/test/java/com/agenticstore/TestcontainersConfig.java`:

```java
package com.agenticstore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
```

Create `backend/src/test/java/com/agenticstore/AgenticStoreApplicationTests.java`:

```java
package com.agenticstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class AgenticStoreApplicationTests {

    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=AgenticStoreApplicationTests -q 2>&1 | tail -10
```

Expected: fails because JPA entities are not defined yet and there are no migrations.

- [ ] **Step 3: Create `V1__create_users.sql`**

```sql
CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER'
                             CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'CUSTOMER')),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
```

- [ ] **Step 4: Create `V2__create_products.sql`**

```sql
CREATE TABLE products (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          NUMERIC(10, 2) NOT NULL CONSTRAINT products_price_positive CHECK (price > 0),
    stock_quantity INT            NOT NULL DEFAULT 0
                                  CONSTRAINT products_stock_non_negative CHECK (stock_quantity >= 0),
    active         BOOLEAN        NOT NULL DEFAULT true,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now()
);
```

- [ ] **Step 5: Create `V3__create_orders.sql`**

```sql
CREATE TABLE orders (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL REFERENCES users (id),
    total_amount NUMERIC(10, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT now()
);
```

- [ ] **Step 6: Create `V4__create_order_items.sql`**

```sql
CREATE TABLE order_items (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID           NOT NULL REFERENCES orders (id),
    product_id UUID           NOT NULL REFERENCES products (id),
    quantity   INT            NOT NULL CONSTRAINT order_items_quantity_positive CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL CONSTRAINT order_items_unit_price_positive CHECK (unit_price > 0)
);
```

The context test still won't pass yet (JPA entities don't exist), but the migrations are ready. The test will pass after Task 4.

- [ ] **Step 7: Commit**

```bash
git add backend/src
git commit -m "feat: add Flyway migrations for all four tables"
```

---

## Task 4: JPA entities

**Files:**
- Create: `backend/src/main/java/com/agenticstore/entity/UserRole.java`
- Create: `backend/src/main/java/com/agenticstore/entity/User.java`
- Create: `backend/src/main/java/com/agenticstore/entity/Product.java`
- Create: `backend/src/main/java/com/agenticstore/entity/Order.java`
- Create: `backend/src/main/java/com/agenticstore/entity/OrderItem.java`

- [ ] **Step 1: Create `UserRole.java`**

```java
package com.agenticstore.entity;

public enum UserRole {
    ADMIN, CUSTOMER
}
```

- [ ] **Step 2: Create `User.java`**

```java
package com.agenticstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create `Product.java`**

Note: the field is named `active` (maps to DB column `active`); Lombok generates `isActive()` getter for boolean fields.

```java
package com.agenticstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: Create `Order.java`**

```java
package com.agenticstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 5: Create `OrderItem.java`**

```java
package com.agenticstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;
}
```

- [ ] **Step 6: Run the context load test**

```bash
cd backend && ./mvnw test -Dtest=AgenticStoreApplicationTests -q
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`

If it fails with "Schema-validation: missing table", confirm migration file names exactly match `V1__create_users.sql` (double underscore).

- [ ] **Step 7: Commit**

```bash
git add backend/src
git commit -m "feat: add JPA entities for User, Product, Order, OrderItem"
```

---

## Task 5: Repositories

**Files:**
- Create: `backend/src/main/java/com/agenticstore/repository/UserRepository.java`
- Create: `backend/src/main/java/com/agenticstore/repository/ProductRepository.java`
- Create: `backend/src/main/java/com/agenticstore/repository/OrderRepository.java`
- Create: `backend/src/main/java/com/agenticstore/repository/OrderItemRepository.java`

- [ ] **Step 1: Create `UserRepository.java`**

```java
package com.agenticstore.repository;

import com.agenticstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
```

- [ ] **Step 2: Create `ProductRepository.java`**

```java
package com.agenticstore.repository;

import com.agenticstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByActiveTrue();
}
```

- [ ] **Step 3: Create `OrderRepository.java`**

```java
package com.agenticstore.repository;

import com.agenticstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findAllByUserId(UUID userId);
}
```

- [ ] **Step 4: Create `OrderItemRepository.java`**

```java
package com.agenticstore.repository;

import com.agenticstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {}
```

- [ ] **Step 5: Run the context test to confirm repositories wire up**

```bash
cd backend && ./mvnw test -Dtest=AgenticStoreApplicationTests -q
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/src
git commit -m "feat: add Spring Data JPA repositories"
```

---

## Task 6: JWT security infrastructure

**Files:**
- Create: `backend/src/main/java/com/agenticstore/security/UserPrincipal.java`
- Create: `backend/src/main/java/com/agenticstore/security/JwtUtil.java`
- Create: `backend/src/main/java/com/agenticstore/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/agenticstore/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/agenticstore/security/JwtUtilTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/security/JwtUtilTest.java`:

```java
package com.agenticstore.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
            86400000L
        );
    }

    @Test
    void generateAndParse_roundTrip() {
        String token = jwtUtil.generateToken(userId, "user@example.com", "CUSTOMER");
        var claims = jwtUtil.parseToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("user@example.com", claims.get("email", String.class));
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken(userId, "user@example.com", "CUSTOMER");
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_withTamperedToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not.a.real.token"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=JwtUtilTest -q 2>&1 | tail -5
```

Expected: compilation error — `JwtUtil` does not exist.

- [ ] **Step 3: Create `UserPrincipal.java`**

```java
package com.agenticstore.security;

import java.util.UUID;

public record UserPrincipal(UUID id, String email, String role) {}
```

- [ ] **Step 4: Create `JwtUtil.java`**

```java
package com.agenticstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String email, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=JwtUtilTest -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: Create `JwtAuthFilter.java`**

```java
package com.agenticstore.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims = jwtUtil.parseToken(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String role = claims.get("role", String.class);
        UserPrincipal principal = new UserPrincipal(userId, claims.get("email", String.class), role);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 7: Create `SecurityConfig.java`**

```java
package com.agenticstore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 8: Run full test suite**

```bash
cd backend && ./mvnw test -q
```

Expected: all tests pass including `AgenticStoreApplicationTests` and `JwtUtilTest`.

- [ ] **Step 9: Commit**

```bash
git add backend/src
git commit -m "feat: add JWT auth infrastructure (JwtUtil, JwtAuthFilter, SecurityConfig)"
```

---

## Task 7: Auth service and DTOs

**Files:**
- Create: `backend/src/main/java/com/agenticstore/dto/auth/RegisterRequest.java`
- Create: `backend/src/main/java/com/agenticstore/dto/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/agenticstore/dto/auth/AuthResponse.java`
- Create: `backend/src/main/java/com/agenticstore/service/AuthService.java`
- Create: `backend/src/test/java/com/agenticstore/service/AuthServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/service/AuthServiceTest.java`:

```java
package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    @Test
    void register_withNewEmail_returnsSuccessWithToken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        User saved = User.builder()
                .id(UUID.randomUUID()).email("new@example.com")
                .name("Alice").role(UserRole.CUSTOMER).build();
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt.token.here");

        Result<AuthResponse> result = authService.register(
                new RegisterRequest("new@example.com", "password123", "Alice"));

        assertInstanceOf(Result.Success.class, result);
        assertEquals("jwt.token.here", ((Result.Success<AuthResponse>) result).value().token());
    }

    @Test
    void register_withExistingEmail_returnsFailure409() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        Result<AuthResponse> result = authService.register(
                new RegisterRequest("taken@example.com", "password123", "Alice"));

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(409, ((Result.Failure<AuthResponse>) result).httpStatus());
    }

    @Test
    void login_withValidCredentials_returnsSuccessWithToken() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hashed").name("Alice").role(UserRole.CUSTOMER).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt.token.here");

        Result<AuthResponse> result = authService.login(
                new LoginRequest("user@example.com", "password123"));

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void login_withWrongPassword_returnsFailure401() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hashed").name("Alice").role(UserRole.CUSTOMER).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        Result<AuthResponse> result = authService.login(
                new LoginRequest("user@example.com", "wrong"));

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(401, ((Result.Failure<AuthResponse>) result).httpStatus());
    }

    @Test
    void login_withUnknownEmail_returnsFailure401() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Result<AuthResponse> result = authService.login(
                new LoginRequest("nobody@example.com", "password123"));

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(401, ((Result.Failure<AuthResponse>) result).httpStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=AuthServiceTest -q 2>&1 | tail -5
```

Expected: compilation error — `AuthService`, `RegisterRequest`, etc. do not exist.

- [ ] **Step 3: Create DTOs**

`RegisterRequest.java`:
```java
package com.agenticstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name
) {}
```

`LoginRequest.java`:
```java
package com.agenticstore.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
```

`AuthResponse.java`:
```java
package com.agenticstore.dto.auth;

public record AuthResponse(String token) {}
```

- [ ] **Step 4: Create `AuthService.java`**

```java
package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.entity.User;
import com.agenticstore.entity.UserRole;
import com.agenticstore.repository.UserRepository;
import com.agenticstore.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Result<AuthResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return Result.failure("Email already in use", 409);
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(UserRole.CUSTOMER)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return Result.success(new AuthResponse(token));
    }

    public Result<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(u -> {
                    String token = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name());
                    return Result.<AuthResponse>success(new AuthResponse(token));
                })
                .orElseGet(() -> Result.failure("Invalid credentials", 401));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=AuthServiceTest -q
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/src
git commit -m "feat: add AuthService with register and login returning Result<T>"
```

---

## Task 8: Auth controller

**Files:**
- Create: `backend/src/main/java/com/agenticstore/controller/AuthController.java`
- Create: `backend/src/test/java/com/agenticstore/controller/AuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/controller/AuthControllerTest.java`:

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void register_withValidBody_returns201WithToken() throws Exception {
        when(authService.register(any())).thenReturn(Result.success(new AuthResponse("my.jwt.token")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"alice@example.com","password":"password123","name":"Alice"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("my.jwt.token"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenReturn(Result.failure("Email already in use", 409));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"alice@example.com","password":"password123","name":"Alice"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        when(authService.login(any())).thenReturn(Result.success(new AuthResponse("my.jwt.token")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"alice@example.com","password":"password123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("my.jwt.token"));
    }

    @Test
    void login_withBadCredentials_returns401() throws Exception {
        when(authService.login(any())).thenReturn(Result.failure("Invalid credentials", 401));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"alice@example.com","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=AuthControllerTest -q 2>&1 | tail -5
```

Expected: compilation error — `AuthController` does not exist.

- [ ] **Step 3: Create `AuthController.java`**

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.auth.AuthResponse;
import com.agenticstore.dto.auth.LoginRequest;
import com.agenticstore.dto.auth.RegisterRequest;
import com.agenticstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return switch (authService.register(request)) {
            case Result.Success<AuthResponse> s -> ResponseEntity.status(201).body(s.value());
            case Result.Failure<AuthResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return switch (authService.login(request)) {
            case Result.Success<AuthResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<AuthResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=AuthControllerTest -q
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -m "feat: add AuthController for register and login endpoints"
```

---

## Task 9: Product service and DTOs

**Files:**
- Create: `backend/src/main/java/com/agenticstore/dto/product/ProductRequest.java`
- Create: `backend/src/main/java/com/agenticstore/dto/product/ProductResponse.java`
- Create: `backend/src/main/java/com/agenticstore/service/ProductService.java`
- Create: `backend/src/test/java/com/agenticstore/service/ProductServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/service/ProductServiceTest.java`:

```java
package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.entity.Product;
import com.agenticstore.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductService productService;

    private Product buildProduct(UUID id, boolean active) {
        return Product.builder()
                .id(id).name("Shirt").description("A shirt")
                .price(new BigDecimal("19.99")).stockQuantity(10)
                .active(active).build();
    }

    @Test
    void listActive_returnsOnlyActiveProducts() {
        Product p = buildProduct(UUID.randomUUID(), true);
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(p));

        List<ProductResponse> results = productService.listActive();

        assertEquals(1, results.size());
        assertEquals("Shirt", results.get(0).name());
    }

    @Test
    void getById_withExistingId_returnsSuccess() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.of(buildProduct(id, true)));

        Result<ProductResponse> result = productService.getById(id);

        assertInstanceOf(Result.Success.class, result);
        assertEquals(id, ((Result.Success<ProductResponse>) result).value().id());
    }

    @Test
    void getById_withMissingId_returnsFailure404() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Result<ProductResponse> result = productService.getById(id);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(404, ((Result.Failure<ProductResponse>) result).httpStatus());
    }

    @Test
    void create_savesAndReturnsProduct() {
        UUID id = UUID.randomUUID();
        Product saved = buildProduct(id, true);
        when(productRepository.save(any())).thenReturn(saved);

        Result<ProductResponse> result = productService.create(
                new ProductRequest("Shirt", "A shirt", new BigDecimal("19.99"), 10));

        assertInstanceOf(Result.Success.class, result);
        assertEquals(id, ((Result.Success<ProductResponse>) result).value().id());
    }

    @Test
    void softDelete_withExistingId_setsActiveFalseAndSaves() {
        UUID id = UUID.randomUUID();
        Product product = buildProduct(id, true);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        Result<Void> result = productService.softDelete(id);

        assertInstanceOf(Result.Success.class, result);
        assertFalse(product.isActive());
        verify(productRepository).save(product);
    }

    @Test
    void softDelete_withMissingId_returnsFailure404() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Result<Void> result = productService.softDelete(id);

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(404, ((Result.Failure<Void>) result).httpStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=ProductServiceTest -q 2>&1 | tail -5
```

Expected: compilation error — `ProductService`, `ProductRequest`, `ProductResponse` do not exist.

- [ ] **Step 3: Create DTOs**

`ProductRequest.java`:
```java
package com.agenticstore.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotNull @PositiveOrZero Integer stockQuantity
) {}
```

`ProductResponse.java`:
```java
package com.agenticstore.dto.product;

import com.agenticstore.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
```

- [ ] **Step 4: Create `ProductService.java`**

```java
package com.agenticstore.service;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.entity.Product;
import com.agenticstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> listActive() {
        return productRepository.findAllByActiveTrue().stream()
                .map(ProductResponse::from).toList();
    }

    public Result<ProductResponse> getById(UUID id) {
        return productRepository.findById(id)
                .map(p -> Result.<ProductResponse>success(ProductResponse.from(p)))
                .orElseGet(() -> Result.failure("Product not found", 404));
    }

    @Transactional
    public Result<ProductResponse> create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();
        return Result.success(ProductResponse.from(productRepository.save(product)));
    }

    @Transactional
    public Result<ProductResponse> update(UUID id, ProductRequest request) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setName(request.name());
                    p.setDescription(request.description());
                    p.setPrice(request.price());
                    p.setStockQuantity(request.stockQuantity());
                    return Result.<ProductResponse>success(ProductResponse.from(productRepository.save(p)));
                })
                .orElseGet(() -> Result.failure("Product not found", 404));
    }

    @Transactional
    public Result<Void> softDelete(UUID id) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setActive(false);
                    productRepository.save(p);
                    return Result.<Void>success(null);
                })
                .orElseGet(() -> Result.failure("Product not found", 404));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=ProductServiceTest -q
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/src
git commit -m "feat: add ProductService with CRUD and soft-delete returning Result<T>"
```

---

## Task 10: Product controller

**Files:**
- Create: `backend/src/main/java/com/agenticstore/controller/ProductController.java`
- Create: `backend/src/test/java/com/agenticstore/controller/ProductControllerTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/controller/ProductControllerTest.java`:

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;

    private ProductResponse sampleResponse(UUID id) {
        return new ProductResponse(id, "Shirt", "A shirt",
                new BigDecimal("19.99"), 10, true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listProducts_returns200WithList() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.listActive()).thenReturn(List.of(sampleResponse(id)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Shirt"));
    }

    @Test
    void getProduct_withExistingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(Result.success(sampleResponse(id)));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shirt"));
    }

    @Test
    void getProduct_withMissingId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(Result.failure("Product not found", 404));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void createProduct_withValidBody_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.create(any())).thenReturn(Result.success(sampleResponse(id)));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"Shirt","description":"A shirt","price":19.99,"stockQuantity":10}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Shirt"));
    }

    @Test
    void deleteProduct_withExistingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.softDelete(id)).thenReturn(Result.success(null));

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=ProductControllerTest -q 2>&1 | tail -5
```

Expected: compilation error — `ProductController` does not exist.

- [ ] **Step 3: Create `ProductController.java`**

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.product.ProductRequest;
import com.agenticstore.dto.product.ProductResponse;
import com.agenticstore.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listActive();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return switch (productService.getById(id)) {
            case Result.Success<ProductResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<ProductResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProductRequest request) {
        return switch (productService.create(request)) {
            case Result.Success<ProductResponse> s -> ResponseEntity.status(201).body(s.value());
            case Result.Failure<ProductResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return switch (productService.update(id, request)) {
            case Result.Success<ProductResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<ProductResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable UUID id) {
        return switch (productService.softDelete(id)) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=ProductControllerTest -q
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -m "feat: add ProductController for CRUD endpoints"
```

---

## Task 11: Order service and DTOs

**Files:**
- Create: `backend/src/main/java/com/agenticstore/dto/order/OrderItemRequest.java`
- Create: `backend/src/main/java/com/agenticstore/dto/order/PlaceOrderRequest.java`
- Create: `backend/src/main/java/com/agenticstore/dto/order/OrderItemResponse.java`
- Create: `backend/src/main/java/com/agenticstore/dto/order/OrderResponse.java`
- Create: `backend/src/main/java/com/agenticstore/service/OrderService.java`
- Create: `backend/src/test/java/com/agenticstore/service/OrderServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/service/OrderServiceTest.java`:

```java
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @InjectMocks OrderService orderService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
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
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(buildSavedOrder(user, new BigDecimal("39.98")));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(productId, 2)));
        Result<OrderResponse> result = orderService.placeOrder(userId, request);

        assertInstanceOf(Result.Success.class, result);
        assertEquals(8, product.getStockQuantity());
        verify(productRepository).save(product);
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
        when(productRepository.save(any())).thenReturn(product);

        // Capture the order passed to save to inspect items
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
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=OrderServiceTest -q 2>&1 | tail -5
```

Expected: compilation error — `OrderService`, order DTOs do not exist.

- [ ] **Step 3: Create order DTOs**

`OrderItemRequest.java`:
```java
package com.agenticstore.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity
) {}
```

`PlaceOrderRequest.java`:
```java
package com.agenticstore.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty List<@Valid OrderItemRequest> items
) {}
```

`OrderItemResponse.java`:
```java
package com.agenticstore.dto.order;

import com.agenticstore.entity.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice());
    }
}
```

`OrderResponse.java`:
```java
package com.agenticstore.dto.order;

import com.agenticstore.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
```

- [ ] **Step 4: Create `OrderService.java`**

```java
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Result<OrderResponse> placeOrder(UUID userId, PlaceOrderRequest request) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.failure("User not found", 404);
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId()).orElse(null);
            if (product == null || !product.isActive()) {
                return Result.failure("Product not found: " + itemReq.productId(), 404);
            }
            if (product.getStockQuantity() < itemReq.quantity()) {
                return Result.failure("Insufficient stock for: " + product.getName(), 422);
            }
            product.setStockQuantity(product.getStockQuantity() - itemReq.quantity());
            productRepository.save(product);

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

        return Result.success(OrderResponse.from(orderRepository.save(order)));
    }

    public List<OrderResponse> listForUser(UUID userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(OrderResponse::from).toList();
    }

    public Result<OrderResponse> getForUser(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    if (!order.getUser().getId().equals(userId)) {
                        return Result.<OrderResponse>failure("Forbidden", 403);
                    }
                    return Result.<OrderResponse>success(OrderResponse.from(order));
                })
                .orElseGet(() -> Result.failure("Order not found", 404));
    }

    public List<OrderResponse> listAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from).toList();
    }

    public Result<OrderResponse> getAny(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(o -> Result.<OrderResponse>success(OrderResponse.from(o)))
                .orElseGet(() -> Result.failure("Order not found", 404));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && ./mvnw test -Dtest=OrderServiceTest -q
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/src
git commit -m "feat: add OrderService with transactional place-order, stock decrement, and price snapshot"
```

---

## Task 12: Order controllers

**Files:**
- Create: `backend/src/main/java/com/agenticstore/controller/OrderController.java`
- Create: `backend/src/main/java/com/agenticstore/controller/AdminOrderController.java`
- Create: `backend/src/test/java/com/agenticstore/controller/OrderControllerTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/agenticstore/controller/OrderControllerTest.java`:

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.security.UserPrincipal;
import com.agenticstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;

    private UUID userId;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new UserPrincipal(userId, "user@example.com", "CUSTOMER");
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private OrderResponse sampleOrder(UUID orderId) {
        return new OrderResponse(orderId, userId, List.of(),
                new BigDecimal("19.99"), LocalDateTime.now());
    }

    @Test
    void placeOrder_withValidBody_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.placeOrder(eq(userId), any()))
                .thenReturn(Result.success(sampleOrder(orderId)));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"items":[{"productId":"%s","quantity":1}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void placeOrder_withInsufficientStock_returns422() throws Exception {
        when(orderService.placeOrder(eq(userId), any()))
                .thenReturn(Result.failure("Insufficient stock for: Shirt", 422));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"items":[{"productId":"%s","quantity":100}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient stock for: Shirt"));
    }

    @Test
    void listOrders_returns200WithOwnOrders() throws Exception {
        when(orderService.listForUser(userId))
                .thenReturn(List.of(sampleOrder(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void getOrder_withForbiddenOrder_returns403() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getForUser(orderId, userId))
                .thenReturn(Result.failure("Forbidden", 403));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -Dtest=OrderControllerTest -q 2>&1 | tail -5
```

Expected: compilation error — `OrderController` does not exist.

- [ ] **Step 3: Create `OrderController.java`**

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.dto.order.PlaceOrderRequest;
import com.agenticstore.security.UserPrincipal;
import com.agenticstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
```

- [ ] **Step 4: Create `AdminOrderController.java`**

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
import com.agenticstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> listAll() {
        return orderService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return switch (orderService.getAny(id)) {
            case Result.Success<OrderResponse> s -> ResponseEntity.ok(s.value());
            case Result.Failure<OrderResponse> f -> ResponseEntity.status(f.httpStatus()).body(Map.of("error", f.error()));
        };
    }
}
```

- [ ] **Step 5: Create `AdminOrderControllerTest.java`**

Create `backend/src/test/java/com/agenticstore/controller/AdminOrderControllerTest.java`:

```java
package com.agenticstore.controller;

import com.agenticstore.common.Result;
import com.agenticstore.dto.order.OrderResponse;
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
```

- [ ] **Step 6: Run all tests**

```bash
cd backend && ./mvnw test -q
```

Expected: all tests pass. Final count should be approximately 30 tests, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add backend/src
git commit -m "feat: add OrderController and AdminOrderController"
```

---

## Task 13: Docker setup

**Files:**
- Create: `backend/Dockerfile`
- Create: `docker-compose.yml` (repo root)
- Create: `.env.example` (repo root)

- [ ] **Step 1: Create `backend/Dockerfile`**

```dockerfile
FROM maven:3.9.7-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create `docker-compose.yml` at the repo root**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-agenticstore}
      POSTGRES_USER: ${DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-postgres}"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: ${DB_NAME:-agenticstore}
      DB_USERNAME: ${DB_USERNAME:-postgres}
      DB_PASSWORD: ${DB_PASSWORD:-postgres}
      JWT_SECRET: ${JWT_SECRET:-change-me-in-production-minimum-32-chars-long}
    depends_on:
      db:
        condition: service_healthy

volumes:
  postgres-data:
```

- [ ] **Step 3: Create `.env.example` at the repo root**

```
DB_NAME=agenticstore
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=change-me-in-production-minimum-32-chars-long
```

- [ ] **Step 4: Verify Docker build compiles**

```bash
cd /path/to/agentic-store && docker compose build backend 2>&1 | tail -5
```

Expected: `=> exporting to image` with no errors.

- [ ] **Step 5: Commit**

```bash
git add backend/Dockerfile docker-compose.yml .env.example
git commit -m "feat: add Dockerfile and docker-compose for backend and database"
```

---

## Spec Coverage Checklist

| Spec requirement | Implemented in |
|---|---|
| `users` table | Task 3 (V1 migration) + Task 4 (entity) |
| `products` table | Task 3 (V2 migration) + Task 4 (entity) |
| `orders` table | Task 3 (V3 migration) + Task 4 (entity) |
| `order_items` table | Task 3 (V4 migration) + Task 4 (entity) |
| `Result<T>` pattern | Task 2 |
| Auth register + login | Task 7 (service) + Task 8 (controller) |
| Product CRUD + soft delete | Task 9 (service) + Task 10 (controller) |
| Place order (transactional) | Task 11 (service) |
| Stock decrement on order | Task 11 |
| Insufficient stock → reject entire order | Task 11 |
| `unit_price` snapshot at order time | Task 11 |
| `total_amount` denormalized on order | Task 11 |
| `is_active` filter on product list | Task 9 |
| `GET /orders/{id}` → 403 for other users' orders | Task 11 (service) + Task 12 (controller) |
| Admin order list + detail | Task 12 (`AdminOrderController`) |
| Docker + docker-compose | Task 13 |
| SOLID / single-responsibility | Applied throughout — one service per domain, repos are interfaces |
| Flyway for all schema changes | Task 3 |
