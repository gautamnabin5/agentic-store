# Data Model Design — Agentic Store

**Date:** 2026-05-06
**Status:** Approved
**Scope:** Backend data model for a simple online store (products, users, orders, inventory)

---

## Overview

A PostgreSQL-backed data model for a store API where customers browse products, place multi-item orders, and view their order history. Admins manage the product catalog and inventory. Schema migrations are managed via Flyway.

---

## Tables

### `users`

| Column | Type | Notes |
|---|---|---|
| `id` | `uuid` PK | Generated |
| `email` | `varchar` | Unique, not null |
| `password_hash` | `varchar` | Not null |
| `name` | `varchar` | Not null |
| `role` | `enum(ADMIN, CUSTOMER)` | Default `CUSTOMER` |
| `created_at` | `timestamp` | Default now() |

### `products`

| Column | Type | Notes |
|---|---|---|
| `id` | `uuid` PK | Generated |
| `name` | `varchar` | Not null |
| `description` | `text` | Nullable |
| `price` | `numeric(10,2)` | Not null, > 0 |
| `stock_quantity` | `int` | Not null, >= 0 |
| `is_active` | `boolean` | Default `true` |
| `created_at` | `timestamp` | Default now() |
| `updated_at` | `timestamp` | Updated on every write |

### `orders`

| Column | Type | Notes |
|---|---|---|
| `id` | `uuid` PK | Generated |
| `user_id` | `uuid` FK → `users.id` | Not null |
| `total_amount` | `numeric(10,2)` | Denormalized; computed at creation, never updated |
| `created_at` | `timestamp` | Default now() |

### `order_items`

| Column | Type | Notes |
|---|---|---|
| `id` | `uuid` PK | Generated |
| `order_id` | `uuid` FK → `orders.id` | Not null |
| `product_id` | `uuid` FK → `products.id` | Not null |
| `quantity` | `int` | Not null, > 0 |
| `unit_price` | `numeric(10,2)` | Snapshot of `products.price` at order time; never updated |

---

## Business Rules

### Placing an Order
1. Validate all requested products exist and `is_active = true`.
2. Validate `stock_quantity >= requested quantity` for every item. If any item fails, reject the entire order (no partial fulfillment).
3. Decrement `stock_quantity` on each product atomically within a single transaction.
4. Snapshot `products.price` → `order_items.unit_price` for each item.
5. Compute and store `orders.total_amount = SUM(unit_price * quantity)`.

### Inventory Management
- Admins update `products.stock_quantity` directly (restock). No audit log at this stage.
- `stock_quantity` must never go below 0 — enforced at the service layer and via a DB check constraint.

### Soft Delete
- Products are never hard-deleted. Admin DELETE sets `is_active = false`.
- `GET /products` filters to `is_active = true` only.
- `order_items.product_id` may reference an inactive product — intentional for history preservation.

### Price Integrity
- `unit_price` on `order_items` is immutable after creation.
- `total_amount` on `orders` is immutable after creation.
- Changes to `products.price` have no retroactive effect on existing orders.

---

## API Endpoints

Base path: `/api/v1`

### Auth
| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Create customer account |
| `POST` | `/auth/login` | Public | Authenticate, return token |

### Products
| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/products` | Public | List active products |
| `GET` | `/products/{id}` | Public | Get single product |
| `POST` | `/products` | Admin | Create product |
| `PUT` | `/products/{id}` | Admin | Update product (price, description, restock qty) |
| `DELETE` | `/products/{id}` | Admin | Soft-delete (sets `is_active = false`) |

### Orders
| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/orders` | Customer | Place order (list of `{productId, quantity}`) |
| `GET` | `/orders` | Customer | List own orders |
| `GET` | `/orders/{id}` | Customer | Get own order with line items (403 if order belongs to another user) |
| `GET` | `/admin/orders` | Admin | List all orders |
| `GET` | `/admin/orders/{id}` | Admin | Get any order with line items |

---

## Architecture Notes

- All service methods return `Result<T>` — no checked exceptions cross layer boundaries; controllers unwrap and map to HTTP status codes.
- `OrderService.placeOrder()` runs in a single `@Transactional` block covering stock validation, decrement, snapshot, and order creation.
- Repository layer uses Spring Data JPA interfaces; no raw SQL except in Flyway migrations.
- SOLID principles apply throughout: one responsibility per class, dependencies injected via interfaces, open for extension without modifying existing code.

---

## Out of Scope (for this iteration)

- Shipping address
- Order status lifecycle (no PENDING/SHIPPED/DELIVERED states)
- Product categories
- Inventory change audit log
- Payment processing
