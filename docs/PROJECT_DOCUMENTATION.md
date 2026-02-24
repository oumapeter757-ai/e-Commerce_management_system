# E-Commerce Management System — Technical Documentation

**Version:** 1.0.0  
**Date:** February 24, 2026  
**Author:** PetersCode Engineering  
**Repository:** [GitHub — e-Commerce Management System](https://github.com/oumapeter757-ai/e-Commerce_management_system)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Project Structure](#4-project-structure)
5. [Database Design](#5-database-design)
6. [Authentication & Security](#6-authentication--security)
7. [API Reference](#7-api-reference)
8. [Shopping Cart — Kilimall-Style Features](#8-shopping-cart--kilimall-style-features)
9. [Payment Integration — M-PESA](#9-payment-integration--m-pesa)
10. [Caching Strategy — Redis](#10-caching-strategy--redis)
11. [Deployment Guide](#11-deployment-guide)
12. [Environment Variables](#12-environment-variables)
13. [Error Handling](#13-error-handling)
14. [Performance & Scalability](#14-performance--scalability)
15. [Future Roadmap](#15-future-roadmap)

---

## 1. Executive Summary

The **E-Commerce Management System** is an enterprise-grade, full-featured online shopping platform backend built with Spring Boot 4.0. It replicates and extends the functionality found in leading Kenyan e-commerce platforms like **Kilimall**, providing a comprehensive REST API for:

- **Product Management** with variants, categories, flash deals
- **Kilimall-Style Shopping Cart** with item selection, save-for-later, bulk operations, coupon application
- **Wishlist & Recently Viewed** product tracking
- **Order Lifecycle Management** with multi-status workflow
- **M-PESA Payment Integration** (Safaricom STK Push)
- **Inventory Management** with reservation system
- **Review & Rating System** with voting, moderation
- **Real-Time Notifications**
- **Multi-Role Authorization** (Customer, Admin, Seller, Support)
- **Redis Caching** for performance optimization
- **Comprehensive Security** (JWT, rate limiting, input sanitization, HMAC callback verification)

### Key Metrics

| Metric | Value |
|--------|-------|
| Total Java Source Files | 199 |
| REST API Endpoints | 150+ |
| Database Tables | 20+ |
| Controllers | 15 |
| Service Implementations | 19 |
| Flyway Migrations | 15 (V1–V15) |

---

## 2. System Architecture

### 2.1 Architecture Pattern

The system follows the **MVC (Model-View-Controller)** pattern adapted for REST APIs:

```
┌──────────────────────────────────────────────────────────┐
│                     CLIENT (Frontend)                     │
│            React / Angular / Mobile App                   │
└─────────────────────────┬────────────────────────────────┘
                          │ HTTPS (JSON)
                          ▼
┌──────────────────────────────────────────────────────────┐
│                  SECURITY FILTER CHAIN                    │
│  RequestIdFilter → RateLimitingFilter → JwtAuthFilter     │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│                    CONTROLLER LAYER                       │
│  AuthController, CartController, ProductController, etc.  │
│  (Input validation, request mapping, response formatting) │
└─────────────────────────┬────────────────────────────────┘
                          ▼
┌──────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                         │
│  CartServiceImpl, OrderServiceImpl, PaymentServiceImpl    │
│  (Business logic, transaction management, caching)        │
└──────────┬──────────────┴──────────────┬─────────────────┘
           ▼                             ▼
┌────────────────────┐     ┌──────────────────────────────┐
│   REPOSITORY LAYER │     │      EXTERNAL SERVICES       │
│   (Spring Data JPA)│     │  • M-PESA API (Safaricom)    │
│   22 Repositories  │     │  • Email Service (SMTP/Gmail)│
└──────────┬─────────┘     │  • Redis Cache               │
           ▼               └──────────────────────────────┘
┌────────────────────┐
│  MySQL 8.0 Database│
│  (Flyway managed)  │
└────────────────────┘
```

### 2.2 Key Design Principles

- **Layered Architecture** — Strict separation of concerns (Controller → Service → Repository)
- **DTO Pattern** — Request/Response DTOs to decouple API from entities
- **MapStruct Mappers** — Compile-time type-safe mapping between DTOs and entities
- **Optimistic Locking** — `@Version` on critical entities (Cart, Inventory) to prevent concurrent update conflicts
- **Idempotent Operations** — Payment idempotency keys prevent duplicate charges
- **Stateless Authentication** — JWT tokens, no server-side sessions

---

## 3. Technology Stack

### 3.1 Core Framework

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 4.0.1 |
| Web | Spring Web MVC | 7.0.2 |
| Security | Spring Security | 7.0.2 |
| Data Access | Spring Data JPA | 4.0.1 |
| Caching | Spring Data Redis | 4.0.1 |

### 3.2 Database & Persistence

| Component | Technology | Version |
|-----------|-----------|---------|
| Database | MySQL | 8.0 |
| ORM | Hibernate | 7.2.0 |
| Connection Pool | HikariCP | (bundled) |
| Migration | Flyway | 11.14.1 |

### 3.3 Security & Auth

| Component | Technology | Version |
|-----------|-----------|---------|
| JWT | jjwt (io.jsonwebtoken) | 0.12.6 |
| Password Hashing | BCrypt | Strength 12 |
| Rate Limiting | Custom + Redis | — |
| Input Sanitization | OWASP jsoup | — |

### 3.4 Serialization

| Component | Technology | Version |
|-----------|-----------|---------|
| JSON (HTTP) | Jackson 3.x (tools.jackson) | 3.0.3 |
| JSON (Redis) | Jackson 3.x JsonMapper | 3.0.3 |
| Object Mapping | MapStruct | 1.6.3 |

### 3.5 External Integrations

| Integration | Provider | Purpose |
|------------|----------|---------|
| M-PESA STK Push | Safaricom | Mobile money payments |
| SMTP Email | Gmail (App Password) | Verification, notifications |
| Redis | Local / Cloud | Caching, rate limiting |

### 3.6 Dev & Build Tools

| Tool | Technology |
|------|-----------|
| Build | Maven 3.x |
| Code Generation | Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Logging | Logback + SLF4J |
| Monitoring | Spring Boot Actuator |

---

## 4. Project Structure

```
ecommerce_management_system/
├── pom.xml                          # Maven dependencies & build config
├── .env                             # Environment variables (secrets)
├── flyway-repair.sh                 # Database reset utility
├── src/main/java/com/peterscode/ecommerce_management_system/
│   ├── EcommerceManagementSystemApplication.java    # Entry point
│   ├── config/
│   │   ├── AsyncConfig.java         # Async thread pool (5 core, 10 max)
│   │   ├── OpenApiConfig.java       # Swagger/OpenAPI configuration
│   │   ├── RedisConfig.java         # Redis caching + custom serializer
│   │   ├── RestTemplateConfig.java  # HTTP client for M-PESA
│   │   └── SecurityConfig.java      # JWT security filter chain
│   ├── constant/
│   │   └── SecurityConstants.java   # CORS origins, public endpoints
│   ├── controller/                  # 15 REST controllers
│   │   ├── AddressController.java
│   │   ├── AuthController.java
│   │   ├── CartController.java
│   │   ├── CategoryController.java
│   │   ├── CouponController.java
│   │   ├── FlashDealController.java
│   │   ├── InventoryController.java
│   │   ├── NotificationController.java
│   │   ├── OrderController.java
│   │   ├── PaymentController.java
│   │   ├── ProductController.java
│   │   ├── ProductVariantController.java
│   │   ├── RecentlyViewedController.java
│   │   ├── ReviewController.java
│   │   ├── ShippingController.java
│   │   ├── UserController.java
│   │   └── WishlistController.java
│   ├── exception/                   # Global error handling
│   │   ├── BadRequestException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InsufficientStockException.java
│   │   ├── ResourceNotFoundException.java
│   │   └── UnauthorizedException.java
│   ├── mapper/                      # 15 MapStruct mappers
│   ├── model/
│   │   ├── audit/AuditLog.java
│   │   ├── dto/
│   │   │   ├── common/              # Shared DTOs (AddressDTO, CartItemDTO, OrderItemDTO)
│   │   │   ├── request/             # 25+ request DTOs
│   │   │   └── response/            # 20+ response DTOs
│   │   ├── entity/                  # 20 JPA entities
│   │   └── enums/                   # 9 enum types
│   ├── repository/                  # 22 JPA repositories
│   ├── security/
│   │   ├── CustomUserDetailsService.java
│   │   ├── InputSanitizer.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── RateLimitingFilter.java
│   │   ├── RequestIdFilter.java
│   │   └── SecurityUtils.java
│   └── service/
│       ├── impl/                    # 19 service implementations
│       └── mpesa/MpesaTokenService.java
├── src/main/resources/
│   ├── application.yml              # Main configuration
│   ├── application-dev.yml          # Dev profile
│   ├── application-prod.yml         # Production profile
│   └── db/migration/               # 15 Flyway SQL + Java migrations
└── src/main/java/db/migration/
    └── V15__Reconcile_Schema_With_Entities.java  # Java-based migration
```

---

## 5. Database Design

### 5.1 Entity Relationship Overview

The system has **20 database tables** managed by Flyway migrations (V1–V15):

```
┌─────────┐     ┌──────────────┐     ┌──────────┐
│  users  │────<│verification_ │     │ addresses│
│         │     │  tokens      │     │          │
└────┬────┘     └──────────────┘     └──────────┘
     │
     ├──────────────────────────┐
     │                          │
┌────▼────┐              ┌──────▼─────┐
│  carts  │              │  orders    │
│         │              │            │
└────┬────┘              └──────┬─────┘
     │                          │
┌────▼──────┐            ┌──────▼──────┐
│ cart_items │            │ order_items │
└────┬───────┘            └──────┬──────┘
     │                           │
┌────▼───────┐   ┌──────────┐   │     ┌──────────┐
│  products  │──<│categories│   ├────>│ payments │
│            │   └──────────┘   │     └──────────┘
└──┬─────┬──┘                   │
   │     │                ┌─────▼────┐
   │     │                │ shipping │
   │     │                └──────────┘
┌──▼──┐  ┌──▼──────────┐
│prod_│  │  inventory   │
│vari │  │              │
│ants │  └──────────────┘
└─────┘
```

### 5.2 Table Descriptions

| Table | Description | Key Fields |
|-------|------------|------------|
| `users` | User accounts with roles | email, password_hash, role, is_verified |
| `verification_tokens` | Email verification & password reset | token, token_type, expires_at |
| `addresses` | Shipping/billing addresses | user_id, address_type, is_default |
| `categories` | Hierarchical product categories | name, slug, parent_id, is_active |
| `products` | Product catalog | name, sku, price, discount_price, seller_id |
| `product_variants` | Size/color/material variants | product_id, variant_type, variant_value, price_adjustment |
| `inventory` | Stock management with reservations | product_id, total_quantity, reserved_quantity, version |
| `carts` | Shopping carts with metadata | user_id, total_price, expires_at, coupon_code |
| `cart_items` | Items in cart with Kilimall features | cart_id, product_id, selected, saved_for_later, notes |
| `wishlists` | Product wishlists | user_id, product_id |
| `recently_viewed` | Browsing history | user_id, product_id, viewed_at |
| `orders` | Purchase orders | user_id, order_number, status, total_amount |
| `order_items` | Individual items in order | order_id, product_id, quantity, unit_price |
| `payments` | M-PESA payment records | order_id, mpesa_receipt, status, idempotency_key |
| `shipping` | Delivery tracking | order_id, tracking_number, carrier, status |
| `reviews` | Product reviews | user_id, product_id, rating, comment, is_approved |
| `review_votes` | Helpful/unhelpful votes | review_id, user_id, is_helpful |
| `notifications` | In-app notifications | user_id, type, message, is_read |
| `audit_logs` | System audit trail | user_id, action, entity_type, details |
| `coupons` | Discount coupons | code, discount_type, discount_value, min_order |
| `coupon_usages` | Coupon redemption tracking | coupon_id, user_id, order_id |
| `flash_deals` | Time-limited deals | product_id, deal_price, start_time, end_time |

### 5.3 Flyway Migrations

| Version | Description | Type |
|---------|------------|------|
| V1 | Users + Audit tables | SQL |
| V2 | Verification tokens | SQL |
| V3 | Products + Categories | SQL |
| V4 | Addresses | SQL |
| V5 | Cart + Cart items | SQL |
| V7 | Reviews + Review votes | SQL |
| V8 | Inventory | SQL |
| V9 | Notifications | SQL |
| V10 | Payments | SQL |
| V11 | Shipping | SQL |
| V12 | Cart enhancements + Wishlist + Recently Viewed | SQL |
| V13 | Consolidate PaymentStatus enum | SQL |
| V14 | Coupons + Variants + Flash Deals + Security columns | SQL |
| V15 | Idempotent schema reconciliation (safety net) | Java |

---

## 6. Authentication & Security

### 6.1 Authentication Flow

```
┌──────────┐    POST /api/v1/auth/register    ┌──────────┐
│  Client  │ ─────────────────────────────────>│  Server  │
│          │                                    │          │
│          │    ← 201 Created + Verify Email    │          │
│          │                                    │          │
│          │    GET /api/v1/auth/verify-email   │          │
│          │ ─────────────────────────────────>│          │
│          │                                    │          │
│          │    POST /api/v1/auth/login         │          │
│          │ ─────────────────────────────────>│          │
│          │                                    │          │
│          │    ← 200 { accessToken,            │          │
│          │           refreshToken }           │          │
│          │                                    │          │
│          │    GET /api/v1/products (Bearer)   │          │
│          │ ─────────────────────────────────>│          │
│          │                                    │          │
│          │    POST /api/v1/auth/refresh-token │          │
│          │ ─────────────────────────────────>│          │
│          │    ← new accessToken               │          │
└──────────┘                                    └──────────┘
```

### 6.2 JWT Token Structure

- **Access Token**: 1 hour expiry, contains userId, email, role, IP binding
- **Refresh Token**: 7 days expiry, used to get new access tokens
- **Secret**: 256-bit HMAC-SHA key from `JWT_SECRET` env variable
- **IP Binding**: Tokens are validated against the client IP for extra security

### 6.3 Role-Based Access Control (RBAC)

| Role | Permissions |
|------|------------|
| `CUSTOMER` | Browse, cart, checkout, orders, reviews, wishlist, addresses |
| `SELLER` | All customer + product CRUD, category management, inventory |
| `ADMIN` | Everything — user management, reports, order management, coupons, flash deals, review moderation |
| `SUPPORT` | View orders, process returns (future) |
| `SYSTEM` | Internal inventory operations (reserve/release/confirm) |

### 6.4 Security Layers

| Layer | Implementation | Purpose |
|-------|---------------|---------|
| **Request ID** | `RequestIdFilter` | Assigns UUID to every request for log correlation |
| **Rate Limiting** | `RateLimitingFilter` + Redis | 60 req/min general, 5 req/min payments, 30 req/min cart |
| **JWT Auth** | `JwtAuthenticationFilter` | Validates Bearer token, sets SecurityContext |
| **Input Sanitization** | `InputSanitizer` (jsoup) | Strips HTML/XSS from all user inputs |
| **CORS** | Spring Security CORS | Whitelist allowed origins |
| **CSRF** | Disabled (stateless JWT) | N/A for API-only backend |
| **Security Headers** | CSP, HSTS, X-Frame-Options, X-XSS-Protection | Browser security |
| **Password Hashing** | BCrypt(12) | Adaptive cost factor |
| **Account Lockout** | 5 failed attempts → 30 min lock | Brute force protection |

### 6.5 Rate Limiting Rules

| Endpoint Pattern | Limit | Window |
|-----------------|-------|--------|
| General API | 60 requests | per minute |
| General API | 1000 requests | per hour |
| `/api/v1/payments/**` | 5 requests | per minute |
| `/api/v1/cart/**` (mutations) | 30 requests | per minute |
| `/api/v1/auth/**` | 10 requests | per minute |

---

## 7. API Reference

### Base URL

```
http://localhost:8080/api/v1
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### 7.1 Authentication APIs — `/api/v1/auth`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| POST | `/register/customer` | Public | Register a customer account |
| POST | `/admin/register` | ADMIN | Create admin/seller/support user |
| POST | `/admin/init` | Public | Initialize first admin (one-time) |
| POST | `/login` | Public | Login with email + password |
| POST | `/logout` | Authenticated | Invalidate current session |
| POST | `/refresh-token` | Public | Refresh access token |
| GET | `/verify-email?token=` | Public | Verify email with token |
| POST | `/resend-verification` | Public | Resend verification email |
| POST | `/forgot-password` | Public | Request password reset email |
| POST | `/reset-password` | Public | Reset password with token |

### 7.2 Product APIs — `/api/v1/products`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | Public | List all products (paginated) |
| GET | `/{id}` | Public | Get product by ID |
| GET | `/sku/{sku}` | Public | Get product by SKU |
| GET | `/all` | Public | List all (paginated + sorted) |
| GET | `/category/{categoryId}` | Public | Products by category |
| GET | `/seller/{sellerId}` | Public | Products by seller |
| GET | `/featured` | Public | Featured products |
| GET | `/new-arrivals` | Public | Newest products |
| GET | `/best-sellers` | Public | Top selling products |
| GET | `/on-sale` | Public | Discounted products |
| GET | `/top-rated` | Public | Highest rated products |
| GET | `/search?keyword=` | Public | Full-text search |
| GET | `/filter?category=&brand=&minPrice=&maxPrice=` | Public | Advanced filtering |
| GET | `/brands` | Public | List all brands |
| GET | `/low-stock` | ADMIN/SELLER | Low stock alerts |
| GET | `/out-of-stock` | ADMIN/SELLER | Out of stock items |
| GET | `/stats/summary` | ADMIN | Product statistics |
| POST | `/` | ADMIN/SELLER | Create product |
| PUT | `/{id}` | ADMIN/SELLER | Update product |
| PATCH | `/{id}/stock` | ADMIN/SELLER | Update stock quantity |
| PATCH | `/{id}/status` | ADMIN/SELLER | Activate/deactivate |
| DELETE | `/{id}` | ADMIN | Delete product |

### 7.3 Product Variant APIs — `/api/v1/products/{productId}/variants`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | Public | List active variants |
| GET | `/{variantId}` | Public | Get variant details |
| GET | `/all` | ADMIN/SELLER | List all variants (inc. inactive) |
| POST | `/` | ADMIN/SELLER | Create variant |
| PUT | `/{variantId}` | ADMIN/SELLER | Update variant |
| DELETE | `/{variantId}` | ADMIN/SELLER | Delete variant |

### 7.4 Category APIs — `/api/v1/categories`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | Public | List all categories |
| GET | `/{id}` | Public | Get category by ID |
| GET | `/slug/{slug}` | Public | Get by URL slug |
| GET | `/parent/{parentId}` | Public | Get subcategories |
| GET | `/root` | Public | Top-level categories |
| GET | `/tree` | Public | Full category tree |
| GET | `/search?keyword=` | Public | Search categories |
| POST | `/` | ADMIN/SELLER | Create category |
| PUT | `/{id}` | ADMIN/SELLER | Update category |
| DELETE | `/{id}` | ADMIN | Delete category |

### 7.5 Shopping Cart APIs — `/api/v1/cart`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | CUSTOMER | Get cart with all items |
| GET | `/summary` | CUSTOMER | Cart totals summary |
| POST | `/items` | CUSTOMER | Add item to cart |
| PUT | `/items/{cartItemId}` | CUSTOMER | Update item quantity |
| DELETE | `/items/{cartItemId}` | CUSTOMER | Remove single item |
| DELETE | `/` | CUSTOMER | Clear entire cart |
| PATCH | `/items/select` | CUSTOMER | Toggle item selection |
| POST | `/items/select-all` | CUSTOMER | Select all items |
| POST | `/items/deselect-all` | CUSTOMER | Deselect all items |
| DELETE | `/items/bulk` | CUSTOMER | Bulk remove items |
| POST | `/items/{id}/save-for-later` | CUSTOMER | Move to save-for-later |
| POST | `/items/{id}/move-to-cart` | CUSTOMER | Move back to active cart |
| PATCH | `/items/{id}/notes` | CUSTOMER | Add notes (color/size) |
| POST | `/coupon` | CUSTOMER | Apply coupon code |
| DELETE | `/coupon` | CUSTOMER | Remove coupon |
| GET | `/shipping-estimate` | CUSTOMER | Estimate shipping cost |
| POST | `/merge` | CUSTOMER | Merge guest cart on login |
| GET | `/guest` | Public | Get guest cart (by session) |
| POST | `/guest/items` | Public | Add item to guest cart |

### 7.6 Wishlist APIs — `/api/v1/wishlist`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | CUSTOMER | Get all wishlist items |
| POST | `/` | CUSTOMER | Add product to wishlist |
| DELETE | `/{productId}` | CUSTOMER | Remove from wishlist |
| DELETE | `/` | CUSTOMER | Clear wishlist |
| GET | `/check/{productId}` | CUSTOMER | Check if product is wishlisted |
| GET | `/count` | CUSTOMER | Get wishlist count |
| POST | `/{productId}/move-to-cart` | CUSTOMER | Move wishlist item to cart |

### 7.7 Recently Viewed APIs — `/api/v1/recently-viewed`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | CUSTOMER | Get recently viewed products |
| POST | `/{productId}` | CUSTOMER | Track product view |
| DELETE | `/` | CUSTOMER | Clear history |

### 7.8 Order APIs — `/api/v1/orders`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| POST | `/` | CUSTOMER | Create order |
| POST | `/checkout` | CUSTOMER | Checkout from cart (Kilimall-style) |
| GET | `/my-orders` | CUSTOMER | My orders (paginated) |
| GET | `/{orderId}` | Authenticated | Get order by ID |
| GET | `/number/{orderNumber}` | Authenticated | Get by order number |
| PUT | `/{orderId}/cancel` | Authenticated | Cancel order |
| GET | `/user/date-range` | CUSTOMER | Orders within date range |
| GET | `/user/stats/total-spent` | CUSTOMER | Total spent |
| GET | `/user/stats/count` | CUSTOMER | Total order count |
| GET | `/` | ADMIN | List all orders |
| GET | `/user/{userId}` | ADMIN | Orders by user |
| GET | `/status/{status}` | ADMIN | Orders by status |
| PUT | `/{orderId}/status` | ADMIN | Update order status |
| PUT | `/{orderId}/tracking` | ADMIN | Update tracking info |
| PUT | `/{orderId}/admin-notes` | ADMIN | Add admin notes |
| DELETE | `/{orderId}` | ADMIN | Delete order |

### 7.9 Payment APIs — `/api/v1/payments`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| POST | `/initiate` | CUSTOMER | Initiate M-PESA STK Push |
| POST | `/mpesa/callback` | Public | Safaricom callback (webhook) |
| POST | `/mpesa/timeout` | Public | Safaricom timeout callback |
| GET | `/user` | CUSTOMER | My payments |
| GET | `/order/{orderId}` | CUSTOMER | Payments for order |
| GET | `/{paymentId}` | Authenticated | Payment details |
| GET | `/` | ADMIN | All payments |
| GET | `/mpesa/query/{checkoutRequestId}` | ADMIN | Query M-PESA status |
| GET | `/verify/{orderId}` | Authenticated | Verify payment status |
| POST | `/{paymentId}/refund` | ADMIN | Process refund |
| POST | `/{paymentId}/cancel` | Authenticated | Cancel payment |

### 7.10 Inventory APIs — `/api/v1/inventory`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/{productId}` | Public | Get inventory info |
| GET | `/{productId}/available` | Public | Available stock |
| GET | `/{productId}/total` | Public | Total stock |
| GET | `/{productId}/reserved` | Public | Reserved stock |
| GET | `/{productId}/check-availability` | Public | Check if available |
| GET | `/{productId}/low-stock` | Public | Low stock check |
| GET | `/low-stock` | ADMIN/SELLER | All low-stock products |
| PUT | `/{productId}` | ADMIN/SELLER | Update inventory |
| POST | `/{productId}/restock` | ADMIN/SELLER | Restock product |
| POST | `/{productId}/reserve` | ADMIN/SELLER/SYSTEM | Reserve stock |
| POST | `/{productId}/release` | ADMIN/SELLER/SYSTEM | Release reservation |
| POST | `/{productId}/confirm` | ADMIN/SELLER/SYSTEM | Confirm reservation |

### 7.11 Coupon APIs — `/api/v1/coupons`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/validate?code=` | Public | Validate coupon code |
| GET | `/` | ADMIN | List all coupons |
| GET | `/active` | ADMIN | Active coupons |
| GET | `/{id}` | ADMIN | Coupon details |
| POST | `/` | ADMIN | Create coupon |
| PUT | `/{id}` | ADMIN | Update coupon |
| DELETE | `/{id}` | ADMIN | Delete coupon |

### 7.12 Flash Deal APIs — `/api/v1/flash-deals`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/` | Public | All flash deals |
| GET | `/active` | Public | Currently active deals |
| GET | `/upcoming` | Public | Upcoming deals |
| GET | `/{id}` | Public | Deal details |
| POST | `/` | ADMIN | Create flash deal |
| PUT | `/{id}` | ADMIN | Update deal |
| DELETE | `/{id}` | ADMIN | Delete deal |

### 7.13 Review APIs — `/api/v1/reviews`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/product/{productId}` | Public | Reviews for product |
| GET | `/{reviewId}` | Public | Review details |
| GET | `/pending` | ADMIN | Pending moderation |
| POST | `/` | CUSTOMER | Create review |
| PUT | `/{reviewId}` | CUSTOMER | Update own review |
| DELETE | `/{reviewId}` | CUSTOMER | Delete own review |
| PUT | `/{reviewId}/approve` | ADMIN | Approve review |
| PUT | `/{reviewId}/reject` | ADMIN | Reject review |
| PUT | `/{reviewId}/respond` | ADMIN | Admin response |
| POST | `/{reviewId}/vote` | CUSTOMER | Vote helpful/unhelpful |

### 7.14 Shipping APIs — `/api/v1/shipping`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| POST | `/create` | ADMIN | Create shipment |
| GET | `/` | ADMIN | All shipments |
| GET | `/{id}` | Authenticated | Shipment details |
| GET | `/order/{orderId}` | Authenticated | Shipment for order |
| GET | `/tracking/{trackingNumber}` | Authenticated | Track by number |
| PUT | `/{id}/status` | ADMIN | Update shipping status |

### 7.15 Address APIs — `/api/v1/addresses`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| POST | `/` | CUSTOMER | Create address |
| GET | `/` | CUSTOMER | List my addresses |
| GET | `/{addressId}` | CUSTOMER | Get address |
| GET | `/default` | CUSTOMER | Get default address |
| PUT | `/{addressId}` | CUSTOMER | Update address |
| PUT | `/{addressId}/default` | CUSTOMER | Set as default |
| DELETE | `/{addressId}` | CUSTOMER | Delete address |

### 7.16 Notification APIs — `/api/v1/notifications`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| POST | `/send` | Authenticated | Send notification |
| GET | `/user/{userId}` | Authenticated | Get user notifications |
| GET | `/user/{userId}/unread` | Authenticated | Unread count |
| PATCH | `/{id}/read` | Authenticated | Mark as read |
| PATCH | `/user/{userId}/read-all` | Authenticated | Mark all read |

### 7.17 User Management APIs — `/api/v1/users`

| Method | Endpoint | Auth | Description |
|--------|---------|------|-------------|
| GET | `/profile` | Authenticated | Get own profile |
| PUT | `/profile` | Authenticated | Update profile |
| PUT | `/profile/change-password` | Authenticated | Change password |
| GET | `/admin/` | ADMIN | List all users |
| GET | `/admin/{userId}` | ADMIN | Get user by ID |
| PUT | `/admin/{userId}/role` | ADMIN | Change user role |
| PUT | `/admin/{userId}/status` | ADMIN | Enable/disable user |

---

## 8. Shopping Cart — Kilimall-Style Features

### 8.1 Feature Overview

The cart system mirrors **Kilimall's** shopping experience:

```
┌─────────────────────────────────────────────────┐
│                  SHOPPING CART                    │
│                                                  │
│  ☑ iPhone 15 Pro - KES 189,999                  │
│    Qty: 1  │ Color: Space Black │ ✎ Notes       │
│    [Save for Later] [Remove]                     │
│                                                  │
│  ☑ Samsung Galaxy Buds - KES 12,999             │
│    Qty: 2  │ Selected                           │
│    [Save for Later] [Remove]                     │
│                                                  │
│  ☐ USB-C Cable - KES 499                        │
│    Qty: 3  │ NOT selected for checkout           │
│                                                  │
│  ──── Saved for Later (1) ────                   │
│  📌 Wireless Mouse - KES 2,999                   │
│    [Move to Cart]                                │
│                                                  │
│  ─────────────────────────                       │
│  Coupon: SAVE10 applied (-10%)                   │
│  Selected Items: KES 215,997                     │
│  Estimated Shipping: KES 500                     │
│  Total Savings: KES 21,600                       │
│  ─────────────────────────                       │
│  [Select All] [Deselect All] [Bulk Remove]       │
│  [🛒 Checkout Selected Items]                    │
└─────────────────────────────────────────────────┘
```

### 8.2 Cart Features Matrix

| Feature | Description | Endpoint |
|---------|------------|----------|
| **Item Selection** | Toggle items for checkout (☑/☐) | `PATCH /cart/items/select` |
| **Select All / Deselect All** | Batch selection | `POST /cart/items/select-all` |
| **Save for Later** | Move item out of active cart | `POST /cart/items/{id}/save-for-later` |
| **Move to Cart** | Restore saved item | `POST /cart/items/{id}/move-to-cart` |
| **Item Notes** | Color/size/variant specs | `PATCH /cart/items/{id}/notes` |
| **Bulk Remove** | Delete multiple items at once | `DELETE /cart/items/bulk` |
| **Coupon Application** | Apply/remove discount codes | `POST /cart/coupon` |
| **Shipping Estimate** | Calculate shipping cost | `GET /cart/shipping-estimate` |
| **Guest Cart** | Browse & add without login | `POST /cart/guest/items` |
| **Cart Merge** | Merge guest → user cart on login | `POST /cart/merge` |
| **Price Tracking** | Detect price changes since adding | `price_at_addition` field |
| **Max Buy Quantity** | Per-product purchase limits | `max_buy_quantity` field |
| **Cart Expiry** | Auto-expire stale carts | `CartExpiryService` scheduled |
| **Optimistic Locking** | Concurrent update protection | `@Version` on CartItem |

### 8.3 Checkout Flow (Kilimall-Style)

```
1. Customer selects items in cart (☑ checkbox)
2. POST /api/v1/orders/checkout
   └─ Only selected items are checked out
   └─ Validates stock availability
   └─ Applies coupon if present
   └─ Creates Order + OrderItems
   └─ Reserves inventory
   └─ Removes checked-out items from cart
3. POST /api/v1/payments/initiate
   └─ Sends M-PESA STK Push to customer's phone
4. Customer enters M-PESA PIN on phone
5. Safaricom → POST /api/v1/payments/mpesa/callback
   └─ Updates payment status
   └─ Confirms inventory reservation
   └─ Sends order confirmation notification
```

---

## 9. Payment Integration — M-PESA

### 9.1 Overview

Integration with **Safaricom M-PESA** Daraja API for mobile money payments:

- **STK Push** — Customer receives payment prompt on their phone
- **Callback Processing** — Async webhook from Safaricom
- **Transaction Querying** — Check payment status
- **Idempotency** — Prevents duplicate charges

### 9.2 Payment Flow

```
┌──────────┐         ┌──────────┐         ┌───────────┐
│  Client  │         │  Server  │         │ Safaricom │
│ (App/Web)│         │          │         │   M-PESA  │
└────┬─────┘         └────┬─────┘         └─────┬─────┘
     │  POST /payments/    │                     │
     │  initiate           │                     │
     │────────────────────>│                     │
     │                     │  OAuth Token        │
     │                     │────────────────────>│
     │                     │<────────────────────│
     │                     │                     │
     │                     │  STK Push Request   │
     │                     │────────────────────>│
     │                     │<─── CheckoutReqID ──│
     │<── "Check phone" ───│                     │
     │                     │                     │
     │   ┌─────────────────────────────────┐     │
     │   │ Customer enters PIN on phone    │     │
     │   └─────────────────────────────────┘     │
     │                     │                     │
     │                     │  POST /mpesa/       │
     │                     │  callback           │
     │                     │<────────────────────│
     │                     │  { ResultCode,      │
     │                     │    MpesaReceipt }   │
     │                     │                     │
     │                     │──> Update payment   │
     │                     │──> Confirm order    │
     │                     │──> Send notification│
     │                     │                     │
     │  GET /payments/     │                     │
     │  verify/{orderId}   │                     │
     │────────────────────>│                     │
     │<── payment status ──│                     │
```

### 9.3 M-PESA Configuration

| Property | Description |
|----------|------------|
| `mpesa.api.url` | Safaricom API base URL (sandbox/production) |
| `mpesa.consumer.key` | Daraja app consumer key |
| `mpesa.consumer.secret` | Daraja app consumer secret |
| `mpesa.shortcode` | Business short code |
| `mpesa.passkey` | STK Push passkey |
| `mpesa.callback.url` | Callback URL (must be public, use ngrok for dev) |
| `mpesa.callback.secret` | HMAC secret for callback verification |
| `mpesa.security.allowed-ips` | Safaricom IP whitelist |

### 9.4 Payment Security

- **IP Whitelisting** — Only Safaricom IPs can call callbacks
- **HMAC Signature Verification** — Optional callback signature check
- **Idempotency Keys** — Unique key per payment prevents duplicates
- **Amount Limits** — KES 1 minimum, KES 150,000 maximum
- **Rate Limiting** — 5 payment requests per minute per user

---

## 10. Caching Strategy — Redis

### 10.1 Cache Configuration

| Cache Name | TTL | Purpose |
|-----------|-----|---------|
| `products` | 30 minutes | Product catalog |
| `categories` | 2 hours | Category tree |
| `users` | 15 minutes | User profiles |
| `mpesaToken` | 50 minutes | M-PESA OAuth token |
| `default` | 10 minutes | Everything else |

### 10.2 Redis Usage

| Feature | Key Pattern | Purpose |
|---------|------------|---------|
| **Caching** | `ecommerce:{cacheName}:{key}` | Response caching |
| **Rate Limiting** | `security:ratelimit:{ip}:{window}` | Request throttling |
| **IP Blacklist** | `security:blacklist:{ip}` | Blocked IPs |

### 10.3 Serialization

Uses **Jackson 3.x** `JsonMapper` with polymorphic type info for correct deserialization:
- Type info embedded as `@class` property in JSON
- `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` disabled for ISO-8601 dates
- `BasicPolymorphicTypeValidator` for secure deserialization

---

## 11. Deployment Guide

### 11.1 Prerequisites

- Java 21+ (LTS)
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+

### 11.2 Quick Start

```bash
# 1. Clone
git clone https://github.com/oumapeter757-ai/e-Commerce_management_system.git
cd e-Commerce_management_system

# 2. Configure environment
cp .env.example .env
# Edit .env with your database, Redis, M-PESA credentials

# 3. Start MySQL & Redis
sudo systemctl start mysql redis

# 4. Create database
mysql -u root -p -e "CREATE DATABASE ecommerce_db DEFAULT CHARSET utf8mb4;"

# 5. Build & Run
./mvnw clean spring-boot:run

# App starts on http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 11.3 Fresh Database Setup (Flyway)

```bash
# Option 1: Auto-create (JDBC URL has createDatabaseIfNotExist=true)
./mvnw spring-boot:run

# Option 2: Full reset
./flyway-repair.sh
./mvnw spring-boot:run
```

### 11.4 M-PESA Callback (Development)

```bash
# Use ngrok for public callback URL
ngrok http 8080

# Set FRONTEND_URL in .env to ngrok URL
# e.g., FRONTEND_URL=https://abc123.ngrok-free.app
```

---

## 12. Environment Variables

All sensitive configuration is stored in `.env`:

| Variable | Description | Example |
|----------|------------|---------|
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database name | `ecommerce_db` |
| `DB_USER` | Database user | `peter` |
| `DB_PASSWORD` | Database password | `****` |
| `REDIS_PASSWORD` | Redis password (empty if none) | `` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `465` |
| `MAIL_USERNAME` | Email address | `company@gmail.com` |
| `MAIL_PASSWORD` | Gmail App Password | `xxxx xxxx xxxx xxxx` |
| `JWT_SECRET` | 256-bit JWT signing key | `404E6352...` |
| `MPESA_CONSUMER_KEY` | Daraja consumer key | `RrATT...` |
| `MPESA_CONSUMER_SECRET` | Daraja consumer secret | `zvFyc...` |
| `MPESA_SHORTCODE` | Business shortcode | `174379` |
| `MPESA_PASSKEY` | STK Push passkey | `bfb27...` |
| `MPESA_CALLBACK_SECRET` | HMAC callback secret | `MySuper...` |
| `FRONTEND_URL` | Frontend/callback base URL | `https://....ngrok-free.dev` |

---

## 13. Error Handling

### 13.1 Standard Error Response

All errors return a consistent JSON structure:

```json
{
  "success": false,
  "message": "Product not found with id: 123",
  "data": null,
  "timestamp": "2026-02-24T10:30:00"
}
```

### 13.2 HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET/PUT/PATCH |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid input, validation error |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | Insufficient role permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource, concurrency |
| 422 | Unprocessable | Business rule violation |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Error | Server-side exception |

### 13.3 Validation

All request DTOs use Jakarta Validation:

- `@NotBlank`, `@NotNull` — Required fields
- `@Size(min, max)` — String length limits
- `@Min`, `@Max` — Numeric range
- `@Email` — Email format
- `@Pattern` — Regex validation
- `@Positive` — Must be > 0
- Custom `InputSanitizer` — Strips HTML/XSS

---

## 14. Performance & Scalability

### 14.1 Database Optimization

| Technique | Implementation |
|-----------|---------------|
| **Connection Pooling** | HikariCP (10 max, 5 min-idle) |
| **Batch Operations** | Hibernate batch size 20, ordered inserts/updates |
| **Indexes** | On all foreign keys, search fields, status columns |
| **Lazy Loading** | `FetchType.LAZY` on all `@ManyToOne` relationships |
| **Open-In-View** | Disabled (`spring.jpa.open-in-view: false`) |
| **Optimistic Locking** | `@Version` on Cart, CartItem, Inventory |

### 14.2 Caching

| Layer | Technology | TTL |
|-------|-----------|-----|
| Product catalog | Redis | 30 min |
| Category tree | Redis | 2 hours |
| M-PESA tokens | Redis | 50 min |
| Rate limit counters | Redis | 1-60 min |

### 14.3 Async Processing

| Task | Implementation |
|------|---------------|
| M-PESA callbacks | `CompletableFuture.runAsync()` |
| Email sending | `@Async` with custom thread pool |
| Cart expiry cleanup | `@Scheduled` service |
| Token cleanup | `TokenCleanupService` scheduled |

### 14.4 Response Compression

- HTTP compression enabled (`server.compression.enabled: true`)
- Applicable for JSON responses > 2KB

---

## 15. Future Roadmap

| Feature | Priority | Status |
|---------|----------|--------|
| WebSocket real-time notifications | High | Planned |
| Elasticsearch for product search | High | Planned |
| Multi-vendor marketplace | Medium | Planned |
| Docker & Kubernetes deployment | High | Planned |
| CI/CD pipeline (GitHub Actions) | High | Planned |
| Image upload (S3/CloudFront) | Medium | Planned |
| SMS notifications (Africa's Talking) | Medium | Planned |
| Order tracking map integration | Low | Planned |
| A/B testing for flash deals | Low | Planned |
| GraphQL API layer | Low | Planned |

---

## Appendix A: Order Status Workflow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    │         │           │
    ▼         ▼           ▼
 CANCELLED  CANCELLED   RETURNED
```

## Appendix B: Payment Status Workflow

```
PENDING → COMPLETED
    │
    ├──→ FAILED
    ├──→ CANCELLED
    └──→ REFUNDED
```

---

**Document Version:** 1.0.0  
**Last Updated:** February 24, 2026  
**Prepared for:** Client Presentation  
**Confidentiality:** Internal Use Only

---

*© 2026 PetersCode Engineering. All rights reserved.*

