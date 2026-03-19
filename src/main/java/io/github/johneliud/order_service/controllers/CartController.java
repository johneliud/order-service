package io.github.johneliud.order_service.controllers;

import io.github.johneliud.order_service.dto.ApiResponse;
import io.github.johneliud.order_service.dto.CartItemRequest;
import io.github.johneliud.order_service.dto.CartResponse;
import io.github.johneliud.order_service.dto.UpdateCartItemRequest;
import io.github.johneliud.order_service.services.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("GET /api/cart - userId: {}", userId);
        validateClientAccess(userId, role);

        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("POST /api/cart/items - userId: {}, productId: {}", userId, request.getProductId());
        validateClientAccess(userId, role);

        CartResponse cart = cartService.addItem(userId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Item added to cart", cart));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("PUT /api/cart/items/{} - userId: {}", productId, userId);
        validateClientAccess(userId, role);

        CartResponse cart = cartService.updateItem(userId, productId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart item updated", cart));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable String productId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("DELETE /api/cart/items/{} - userId: {}", productId, userId);
        validateClientAccess(userId, role);

        CartResponse cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("DELETE /api/cart - userId: {}", userId);
        validateClientAccess(userId, role);

        cartService.clearCart(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart cleared", null));
    }

    private void validateClientAccess(String userId, String role) {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (!role.equals("CLIENT")) {
            throw new IllegalArgumentException("Only clients can manage a cart");
        }
    }
}
