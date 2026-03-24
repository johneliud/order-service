# Order Service

Microservice responsible for shopping cart management, checkout, and order lifecycle operations.

## Overview

- **Port**: 8084
- **Technology**: Spring Boot 4.0.3
- **Database**: MongoDB Atlas (collections `carts`, `orders`)
- **Purpose**: Cart CRUD, checkout, order status management

## Features

### Shopping Cart (Clients Only)
- One cart per user (created on first access)
- Add items, update quantities, remove items, clear cart
- Subtotal per item, cart total calculated server-side
- Adding a duplicate product increments its quantity

### Checkout
- Convert cart to one or more orders (split by seller)
- Delivery address validation (fullName, address, city, phone — all required)
- Pay on Delivery payment method
- Cart cleared automatically on success
- Stock decremented via Product Service HTTP call

### Order Management — Buyers
- View own orders with pagination, status filter, and product name search
- Cancel PENDING orders
- Remove CANCELLED or DELIVERED order records

### Order Management — Sellers
- View incoming orders for their products (paginated, filterable)
- Advance order status: PENDING → CONFIRMED → SHIPPED → DELIVERED

## API Endpoints

All requests go through the API Gateway at `http://localhost:8083`.

### Cart Endpoints (CLIENT role required)

#### Get Cart
```http
GET /api/cart
Authorization: Bearer <token>
```

#### Add Item to Cart
```http
POST /api/cart/items
Authorization: Bearer <token>
Content-Type: application/json

{
  "productId": "prod_xyz",
  "productName": "Wireless Headphones",
  "price": 149.99,
  "quantity": 2,
  "imageUrl": "http://localhost:8083/api/media/img_001"
}
```

#### Update Item Quantity
```http
PUT /api/cart/items/{productId}
Authorization: Bearer <token>
Content-Type: application/json

{ "quantity": 3 }
```

#### Remove Item
```http
DELETE /api/cart/items/{productId}
Authorization: Bearer <token>
```

#### Clear Cart
```http
DELETE /api/cart
Authorization: Bearer <token>
```

#### Checkout
```http
POST /api/cart/checkout
Authorization: Bearer <token>
Content-Type: application/json

{
  "deliveryAddress": {
    "fullName": "John Doe",
    "address": "123 Main Street, Apt 4B",
    "city": "Nairobi",
    "phone": "+254712345678"
  },
  "paymentMethod": "PAY_ON_DELIVERY"
}
```

Response (`201 Created`): Array of created orders (one per seller).

### Order Endpoints

#### Get Buyer Orders
```http
GET /api/orders?page=0&size=10&status=PENDING&search=headphones
Authorization: Bearer <token>
```

Query parameters:
- `page` — zero-based page number (default: 0)
- `size` — items per page (default: 10)
- `status` — filter by `PENDING | CONFIRMED | SHIPPED | DELIVERED | CANCELLED`
- `search` — filter by product name within order items

#### Get Seller Orders
```http
GET /api/orders/seller?page=0&size=10&status=PENDING
Authorization: Bearer <seller-token>
```

#### Get Order by ID
```http
GET /api/orders/{orderId}
Authorization: Bearer <token>
```

Accessible by the order's buyer or the seller of the items.

#### Cancel Order (Buyer, PENDING only)
```http
PUT /api/orders/{orderId}/cancel
Authorization: Bearer <client-token>
```

#### Advance Order Status (Seller)
```http
PUT /api/orders/{orderId}/status
Authorization: Bearer <seller-token>
Content-Type: application/json

{ "status": "CONFIRMED" }
```

Valid transitions:
- `PENDING` → `CONFIRMED`
- `CONFIRMED` → `SHIPPED`
- `SHIPPED` → `DELIVERED`

#### Remove Order Record (Buyer)
```http
DELETE /api/orders/{orderId}
Authorization: Bearer <client-token>
```

Only `CANCELLED` or `DELIVERED` orders can be removed.

## Data Models

### Cart
```json
{
  "id": "string",
  "userId": "string (unique per user)",
  "items": [
    {
      "productId": "string",
      "productName": "string",
      "price": "number",
      "quantity": "number",
      "imageUrl": "string (optional)",
      "subtotal": "number"
    }
  ],
  "total": "number",
  "updatedAt": "datetime"
}
```

### Order
```json
{
  "id": "string",
  "userId": "string (buyer)",
  "sellerId": "string",
  "items": [
    {
      "productId": "string",
      "productName": "string",
      "price": "number",
      "quantity": "number"
    }
  ],
  "totalAmount": "number (BigDecimal)",
  "status": "PENDING | CONFIRMED | SHIPPED | DELIVERED | CANCELLED",
  "deliveryAddress": {
    "fullName": "string",
    "address": "string",
    "city": "string",
    "phone": "string"
  },
  "paymentMethod": "PAY_ON_DELIVERY",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

## Configuration

```properties
server.port=8084
spring.mongodb.uri=mongodb+srv://...
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=order-service
jwt.secret=...
jwt.expiration=86400000
```

## Running the Service

```bash
cd backend/order-service
./mvnw spring-boot:run
```

## Kafka Integration

### Producer
Publishes events to two topics when orders are created or updated.

**Topic**: `order-placed`
```json
{
  "orderId": "string",
  "userId": "string",
  "sellerId": "string",
  "items": [...],
  "totalAmount": "number"
}
```

**Topic**: `order-status-changed`
```json
{
  "orderId": "string",
  "userId": "string",
  "sellerId": "string",
  "oldStatus": "string",
  "newStatus": "string"
}
```

### Producer Configuration
- `RETRIES_CONFIG = 3`
- `RETRY_BACKOFF_MS_CONFIG = 1000ms`
- `ACKS_CONFIG = "1"`

## External HTTP Calls

Order Service calls Product Service to decrement stock at checkout via `ProductServiceClient`:

- `getProduct(productId)` — retried up to 3 times (idempotent read)
- `decrementStock(productId, quantity)` — NOT retried (non-idempotent)

## Authorization

Authorization is header-based (no JWT parsing in this service):
- `X-User-Id` — injected by API Gateway from JWT
- `X-User-Role` — injected by API Gateway from JWT

| Endpoint | Required Role |
|----------|--------------|
| Cart operations | CLIENT |
| `GET /api/orders` | CLIENT |
| `PUT /api/orders/{id}/cancel` | CLIENT (own orders) |
| `DELETE /api/orders/{id}` | CLIENT (own orders) |
| `GET /api/orders/seller` | SELLER |
| `PUT /api/orders/{id}/status` | SELLER (own products) |
| `GET /api/orders/{id}` | CLIENT (own) or SELLER (own products) |

## Error Responses

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "errors": { "fieldName": "validation error" }
}
```

Common errors:
- 400 — Empty cart checkout, invalid status transition, item not in cart
- 401 — Missing X-User-Id or X-User-Role header
- 403 — Wrong role or accessing another user's resource
- 500 — Unexpected server error

## Running Tests

```bash
./mvnw test
```

Test coverage includes:
- `CartService`: add, update, remove, clear, checkout (valid and empty cart)
- `OrderService`: list orders with filters, cancel, remove, status transitions
- Controller-level authorization checks
- `OrderServiceApplicationTests`: context load