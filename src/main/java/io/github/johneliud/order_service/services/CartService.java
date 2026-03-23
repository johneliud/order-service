package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.client.ProductServiceClient;
import io.github.johneliud.order_service.dto.*;
import io.github.johneliud.order_service.models.Cart;
import io.github.johneliud.order_service.models.CartItem;
import io.github.johneliud.order_service.repositories.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final OrderService orderService;
    private final ProductServiceClient productServiceClient;

    public CartResponse getCart(String userId) {
        log.info("Fetching cart for userId: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return toCartResponse(cart);
    }

    public CartResponse addItem(String userId, CartItemRequest request) {
        log.info("Adding item productId: {} to cart for userId: {}", request.getProductId(), userId);
        Cart cart = getOrCreateCart(userId);

        ProductDto product = productServiceClient.getProduct(request.getProductId());
        log.info("Fetched authoritative product data: name={}, price={}, sellerId={}", product.getName(), product.getPrice(), product.getUserId());

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + request.getQuantity());
            existing.get().setPrice(product.getPrice());
            existing.get().setProductName(product.getName());
            existing.get().setSellerId(product.getUserId());
            log.info("Incremented quantity for productId: {} in cart for userId: {}", request.getProductId(), userId);
        } else {
            CartItem newItem = new CartItem(
                    request.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    request.getQuantity(),
                    request.getImageUrl(),
                    product.getUserId()
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

    public List<OrderResponse> checkout(String userId, CheckoutRequest request) {
        log.info("Processing checkout for userId: {}", userId);
        Cart cart = getOrCreateCart(userId);

        if (cart.getItems().isEmpty()) {
            log.warn("Checkout failed: cart is empty for userId: {}", userId);
            throw new IllegalArgumentException("Cannot checkout with an empty cart");
        }

        for (CartItem item : cart.getItems()) {
            ProductDto product = productServiceClient.getProduct(item.getProductId());
            if (product.getQuantity() < item.getQuantity()) {
                log.warn("Checkout failed: insufficient stock for productId: {}. Available: {}, requested: {}",
                        item.getProductId(), product.getQuantity(), item.getQuantity());
                throw new IllegalArgumentException(
                        "Insufficient stock for '" + item.getProductName() + "'. " +
                        "Available: " + product.getQuantity() + ", requested: " + item.getQuantity());
            }
        }

        Map<String, List<CartItem>> itemsBySeller = cart.getItems().stream()
                .collect(Collectors.groupingBy(CartItem::getSellerId));

        List<OrderResponse> orders = itemsBySeller.entrySet().stream()
                .map(entry -> orderService.createOrder(userId, entry.getKey(), entry.getValue(), request))
                .collect(Collectors.toList());

        for (CartItem item : cart.getItems()) {
            try {
                productServiceClient.decrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to decrement stock for productId: {} after order creation: {}",
                        item.getProductId(), e.getMessage());
            }
        }

        cart.setItems(new ArrayList<>());
        cartRepository.save(cart);
        log.info("Checkout complete for userId: {}, {} order(s) created, cart cleared", userId, orders.size());

        return orders;
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

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(
                            item.getProductId(),
                            item.getProductName(),
                            item.getPrice(),
                            item.getQuantity(),
                            item.getImageUrl(),
                            subtotal
                    );
                })
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                itemResponses,
                total,
                cart.getUpdatedAt()
        );
    }
}
