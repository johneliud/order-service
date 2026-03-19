package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.dto.CartItemRequest;
import io.github.johneliud.order_service.dto.CartItemResponse;
import io.github.johneliud.order_service.dto.CartResponse;
import io.github.johneliud.order_service.dto.UpdateCartItemRequest;
import io.github.johneliud.order_service.models.Cart;
import io.github.johneliud.order_service.models.CartItem;
import io.github.johneliud.order_service.repositories.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;

    public CartResponse getCart(String userId) {
        log.info("Fetching cart for userId: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return toCartResponse(cart);
    }

    public CartResponse addItem(String userId, CartItemRequest request) {
        log.info("Adding item productId: {} to cart for userId: {}", request.getProductId(), userId);
        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + request.getQuantity());
            log.info("Incremented quantity for productId: {} in cart for userId: {}", request.getProductId(), userId);
        } else {
            CartItem newItem = new CartItem(
                    request.getProductId(),
                    request.getProductName(),
                    request.getPrice(),
                    request.getQuantity(),
                    request.getImageUrl()
            );
            cart.getItems().add(newItem);
            log.info("Added new item productId: {} to cart for userId: {}", request.getProductId(), userId);
        }

        Cart saved = cartRepository.save(cart);
        return toCartResponse(saved);
    }
