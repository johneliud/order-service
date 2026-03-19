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
