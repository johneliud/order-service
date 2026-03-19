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

    public CartResponse updateItem(String userId, String productId, UpdateCartItemRequest request) {
        log.info("Updating item productId: {} in cart for userId: {}", productId, userId);
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Item productId: {} not found in cart for userId: {}", productId, userId);
                    return new IllegalArgumentException("Item not found in cart");
                });

        item.setQuantity(request.getQuantity());
        Cart saved = cartRepository.save(cart);
        log.info("Updated item productId: {} quantity to {} for userId: {}", productId, request.getQuantity(), userId);
        return toCartResponse(saved);
    }

    public CartResponse removeItem(String userId, String productId) {
        log.info("Removing item productId: {} from cart for userId: {}", productId, userId);
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            log.warn("Item productId: {} not found in cart for userId: {}", productId, userId);
            throw new IllegalArgumentException("Item not found in cart");
        }

        Cart saved = cartRepository.save(cart);
        log.info("Removed item productId: {} from cart for userId: {}", productId, userId);
        return toCartResponse(saved);
    }

    public void clearCart(String userId) {
        log.info("Clearing cart for userId: {}", userId);
        Cart cart = getOrCreateCart(userId);
        cart.setItems(new ArrayList<>());
        cartRepository.save(cart);
        log.info("Cart cleared for userId: {}", userId);
    }

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            log.info("No cart found for userId: {}, creating new cart", userId);
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setItems(new ArrayList<>());
            return cartRepository.save(newCart);
        });
    }
