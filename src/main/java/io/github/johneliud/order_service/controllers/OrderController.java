package io.github.johneliud.order_service.controllers;

import io.github.johneliud.order_service.dto.ApiResponse;
import io.github.johneliud.order_service.dto.OrderResponse;
import io.github.johneliud.order_service.dto.PagedResponse;
import io.github.johneliud.order_service.dto.UpdateOrderStatusRequest;
import io.github.johneliud.order_service.exception.ForbiddenException;
import io.github.johneliud.order_service.exception.UnauthorizedException;
import io.github.johneliud.order_service.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("GET /api/orders - userId: {}, page: {}, size: {}, search: {}, status: {}", userId, page, size, search, status);
        requireAuth(userId, role);

        if (!"CLIENT".equals(role)) {
            throw new ForbiddenException("Only clients can access this endpoint");
        }

        PagedResponse<OrderResponse> orders = orderService.getOrdersByBuyer(userId, page, size, search, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Orders retrieved successfully", orders));
    }

    @GetMapping("/seller")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getSellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("GET /api/orders/seller - userId: {}, page: {}, size: {}, search: {}, status: {}", userId, page, size, search, status);
        requireAuth(userId, role);

        if (!"SELLER".equals(role)) {
            throw new ForbiddenException("Only sellers can access this endpoint");
        }

        PagedResponse<OrderResponse> orders = orderService.getOrdersBySeller(userId, page, size, search, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("GET /api/orders/{} - userId: {}", orderId, userId);
        requireAuth(userId, role);

        OrderResponse order = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order retrieved successfully", order));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("PUT /api/orders/{}/cancel - userId: {}", orderId, userId);
        requireAuth(userId, role);

        if (!"CLIENT".equals(role)) {
            throw new ForbiddenException("Only clients can access this endpoint");
        }

        OrderResponse order = orderService.cancelOrder(orderId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order cancelled successfully", order));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> removeOrder(
            @PathVariable String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("DELETE /api/orders/{} - userId: {}", orderId, userId);
        requireAuth(userId, role);

        if (!"CLIENT".equals(role)) {
            throw new ForbiddenException("Only clients can access this endpoint");
        }

        orderService.removeOrder(orderId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order removed successfully", null));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody @Valid UpdateOrderStatusRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("PUT /api/orders/{}/status - userId: {}, status: {}", orderId, userId, request.getStatus());
        requireAuth(userId, role);

        if (!"SELLER".equals(role)) {
            throw new ForbiddenException("Only sellers can access this endpoint");
        }

        OrderResponse order = orderService.updateOrderStatus(orderId, userId, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Order status updated successfully", order));
    }

    private void requireAuth(String userId, String role) {
        if (userId == null || role == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }
}
