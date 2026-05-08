# Frontend Design — Agentic Store

**Date:** 2026-05-07
**Status:** Approved
**Scope:** Customer-facing storefront and admin panel UI

---

## Overview

A general-purpose e-commerce store UI. Customers browse products, manage a cart, place orders, and view order history. Admins manage the product catalog and view all orders. The UI is intentionally a clean CRUD foundation — the future path is adding MCP tools and a chat interface on top of this baseline.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Framework | React 19 + TypeScript |
| Build tool | Vite + pnpm |
| Component library | shadcn/ui (Tailwind CSS v4) |
| Routing | React Router v7 |
| Global state | Zustand with `persist` middleware |
| API calls | Native `fetch` via a shared client wrapper |
| Styling | Dark theme — navy base (`#0f172a`), indigo accents (`#6366f1`) |

---

## Visual Style

**Dark & Bold.** Dark navy backgrounds, indigo primary actions, slate secondary text. Strong typography with clear weight contrast between headings and body. Consistent border-radius and spacing via Tailwind.

Applies to both the storefront and admin panel. Admin panel is visually distinct through layout (sidebar vs top nav) and the `ADMIN` label, not through a different colour scheme.

---

## Application Structure

Two distinct layout contexts sharing the same codebase:

### Customer storefront (`/`)
Top navigation bar. Public routes are accessible without auth; protected routes redirect to `/login` with `?redirect=` preserved.

### Admin panel (`/admin`)
Sidebar navigation. All routes require `role === 'ADMIN'`. Non-admins are redirected to `/`.

---

## Routes

### Customer

| Path | Access | Description |
|---|---|---|
| `/` | Public | Product listing |
| `/products/:id` | Public | Product detail |
| `/login` | Public (redirect if authed) | Login form |
| `/register` | Public (redirect if authed) | Register form |
| `/orders` | Auth required | Order history |
| `/orders/:id` | Auth required | Order detail |

### Admin

| Path | Access | Description |
|---|---|---|
| `/admin` | Admin | Redirect to `/admin/products` |
| `/admin/products` | Admin | Product table |
| `/admin/products/new` | Admin | Create product form |
| `/admin/products/:id/edit` | Admin | Edit product form |
| `/admin/orders` | Admin | All orders table |
| `/admin/orders/:id` | Admin | Order detail (read-only) |

---

## Pages

### Product Listing (`/`)
Grid layout with a sidebar on the left for filters. Sidebar contains: price range slider, in-stock toggle, sort dropdown. All filtering is **client-side** on the already-loaded product list — no filter API endpoints exist in v1.

Product cards show: image placeholder, name, price, availability status, Add to Cart button.

**Stock visibility rule:** Customers never see stock quantities. They see only availability — "In Stock" (button active) or "Out of Stock" (button disabled). The raw number is admin-only.

### Product Detail (`/products/:id`)
Full page. Name, description, price, availability status. Quantity selector (1–available stock, capped client-side). Add to Cart button. Disabled with "Out of Stock" label when stock = 0. Back link to product listing.

Full page (not modal) for shareability and to allow future per-product chat context.

### Login & Register
Centered single-column forms, no navigation bar. Focused layout per Hierarchy principle — nothing competes with the form. After success: redirect to `?redirect` param or `/`.

### Order History (`/orders`)
Table: truncated order ID, date, total amount, item count. Rows are clickable → `/orders/:id`.

### Order Detail (`/orders/:id`)
Line items table: product name, unit price (snapshot at order time), quantity, line total. Order total. Read-only.

### Admin: Product Table (`/admin/products`)
Columns: name, price, stock quantity (colour-coded: green ≥ 10, amber 1–9, red 0), status (Active / Inactive), actions (Edit, Delete).

Delete triggers a confirmation dialog before calling the API — destructive action requires explicit confirmation per Contrast principle.

New Product button → `/admin/products/new`.

### Admin: Product Form (`/admin/products/new` and `/admin/products/:id/edit`)
Full-page form. Fields: name, description, price, stock quantity. Inline validation. On success → redirect to `/admin/products`.

### Admin: Orders Table (`/admin/orders`)
Columns: order ID, customer email, date, total amount, item count. Rows clickable → `/admin/orders/:id`.

### Admin: Order Detail (`/admin/orders/:id`)
Same line-items view as customer order detail. Read-only.

---

## Cart

Slide-out drawer from the right, accessible from any page via the Cart button in the top nav. Not a route.

**Contents:** line items (product name, quantity controls, line subtotal), order total, Place Order button.

**Behaviour:**
- Add to Cart updates the drawer badge count in the nav
- Quantity can be adjusted or item removed inside the drawer
- Place Order calls `POST /orders`, shows loading state
- On success: cart cleared, drawer closed, redirect to `/orders/:id`
- On failure (e.g. insufficient stock): inline error shown inside the drawer, drawer stays open

---

## Global State

### `useAuthStore`
```
{ token: string | null, user: { id, email, role } | null }
login(token, user)  →  clears cart, sets token + user
logout()            →  clears cart, clears token + user
```
Persisted to localStorage via Zustand `persist`.

**Security note:** Token is stored in localStorage. This is accessible to any JavaScript on the page including third-party scripts (XSS risk). Acceptable for a development/demo context. Production path: HttpOnly cookie via Spring Security, with `SameSite=Strict` for CSRF protection.

### `useCartStore`
```
{ items: [{ product: ProductResponse, quantity: number }] }
add(product, quantity)
remove(productId)
updateQuantity(productId, quantity)
clear()
```
Persisted to localStorage via Zustand `persist`.

**Multi-user safety:** `login()` and `logout()` both call `cartStore.clear()`. This ensures no cart data leaks between users on a shared machine.

---

## API Layer (`src/api/`)

| File | Exports |
|---|---|
| `client.ts` | Base `request()` — attaches JWT from auth store, handles 401 (auto-logout) |
| `auth.ts` | `register()`, `login()` |
| `products.ts` | `listProducts()`, `getProduct(id)`, `createProduct()`, `updateProduct()`, `deleteProduct()` |
| `orders.ts` | `placeOrder()`, `listOrders()`, `getOrder(id)`, `listAllOrders()`, `getAnyOrder(id)` |

All functions return typed response objects matching the backend DTOs.

---

## Route Protection

`<ProtectedRoute>` — checks `useAuthStore.token`. Redirects to `/login?redirect=<current-path>` if unauthenticated.

`<AdminRoute>` — checks `useAuthStore.user.role === 'ADMIN'`. Redirects to `/` if not admin.

---

## Navigation

### Customer top nav
- Left: store name / logo → `/`
- Centre: Products → `/`, Orders → `/orders` (hidden if unauthenticated)
- Right: Login + Register (unauthenticated) OR user email + dropdown (authenticated — contains Admin Panel link if admin, Logout)
- Cart icon with item count badge → opens drawer

### Admin sidebar
- Header: `ADMIN` label in indigo
- Section Catalog: Products → `/admin/products`
- Section Commerce: Orders → `/admin/orders`
- Footer: ↩ Back to Storefront → `/`

---

## Out of Scope (v1)

- Search API (client-side filter only)
- Stock quantity visible to customers
- Low-stock warnings ("Only 3 left!")
- Order status lifecycle (no PENDING/SHIPPED/DELIVERED)
- Payment processing
- Product images (placeholder only)
- HttpOnly cookie auth
- Pagination (load all, filter client-side)
