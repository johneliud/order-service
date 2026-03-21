package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.dto.*;
import io.github.johneliud.order_service.models.*;
import io.github.johneliud.order_service.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderResponse createOrder(String userId, String sellerId, List<CartItem> cartItems, CheckoutRequest request) {
        log.info("Creating order for userId: {}, sellerId: {}, items: {}", userId, sellerId, cartItems.size());

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> new OrderItem(
                        item.getProductId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .collect(Collectors.toList());

        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DeliveryAddress deliveryAddress = new DeliveryAddress(
                request.getDeliveryAddress().getFullName(),
                request.getDeliveryAddress().getAddress(),
                request.getDeliveryAddress().getCity(),
                request.getDeliveryAddress().getPhone()
        );

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment method: {}", request.getPaymentMethod());
            throw new IllegalArgumentException("Invalid payment method: " + request.getPaymentMethod());
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setSellerId(sellerId);
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(deliveryAddress);
        order.setPaymentMethod(paymentMethod);

        Order saved = orderRepository.save(order);
        log.info("Order created successfully with ID: {} for userId: {}, sellerId: {}", saved.getId(), userId, sellerId);

        return toOrderResponse(saved);
    }

    public PagedResponse<OrderResponse> getOrdersByBuyer(String userId, int page, int size, String search, String statusStr) {
        log.info("Fetching orders for buyer userId: {}, page: {}, size: {}, search: {}, status: {}", userId, page, size, search, statusStr);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        OrderStatus status = parseStatus(statusStr);

        Page<Order> orderPage;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && status != null) {
            orderPage = orderRepository.findByUserIdAndStatusAndItemsProductNameContainingIgnoreCase(userId, status, search, pageable);
        } else if (hasSearch) {
            orderPage = orderRepository.findByUserIdAndItemsProductNameContainingIgnoreCase(userId, search, pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} orders (page {}/{}) for buyer userId: {}", content.size(), page + 1, orderPage.getTotalPages(), userId);
        return new PagedResponse<>(content, orderPage.getNumber(), orderPage.getSize(), orderPage.getTotalElements(), orderPage.getTotalPages(), orderPage.isLast());
    }

    public OrderResponse getOrderById(String orderId, String requestingUserId) {
        log.info("Fetching order ID: {} for requestingUserId: {}", orderId, requestingUserId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found: {}", orderId);
                    return new IllegalArgumentException("Order not found");
                });

        if (!order.getUserId().equals(requestingUserId) && !order.getSellerId().equals(requestingUserId)) {
            log.warn("Access denied: userId {} attempted to view order {} owned by {} / seller {}", requestingUserId, orderId, order.getUserId(), order.getSellerId());
            throw new IllegalArgumentException("Access denied: You do not have permission to view this order");
        }

        return toOrderResponse(order);
    }

    public PagedResponse<OrderResponse> getOrdersBySeller(String sellerId, int page, int size, String search, String statusStr) {
        log.info("Fetching orders for seller sellerId: {}, page: {}, size: {}, search: {}, status: {}", sellerId, page, size, search, statusStr);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        OrderStatus status = parseStatus(statusStr);

        Page<Order> orderPage;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && status != null) {
            orderPage = orderRepository.findBySellerIdAndStatusAndItemsProductNameContainingIgnoreCase(sellerId, status, search, pageable);
        } else if (hasSearch) {
            orderPage = orderRepository.findBySellerIdAndItemsProductNameContainingIgnoreCase(sellerId, search, pageable);
        } else if (status != null) {
            orderPage = orderRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        } else {
            orderPage = orderRepository.findBySellerId(sellerId, pageable);
        }

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} orders (page {}/{}) for sellerId: {}", content.size(), page + 1, orderPage.getTotalPages(), sellerId);
        return new PagedResponse<>(content, orderPage.getNumber(), orderPage.getSize(), orderPage.getTotalElements(), orderPage.getTotalPages(), orderPage.isLast());
    }

    public OrderResponse cancelOrder(String orderId, String userId) {
        log.info("Cancelling order ID: {} for userId: {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found: {}", orderId);
                    return new IllegalArgumentException("Order not found");
                });

        if (!order.getUserId().equals(userId)) {
            log.warn("Access denied: userId {} attempted to cancel order {} owned by {}", userId, orderId, order.getUserId());
            throw new IllegalArgumentException("Access denied");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Cannot cancel order {}: current status is {}", orderId, order.getStatus());
            throw new IllegalArgumentException("Only PENDING orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order {} cancelled successfully by userId: {}", orderId, userId);
        return toOrderResponse(saved);
    }
    private OrderStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + statusStr);
        }
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new OrderItemResponse(
                            item.getProductId(),
                            item.getProductName(),
                            item.getPrice(),
                            item.getQuantity(),
                            subtotal
                    );
                })
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getSellerId(),
                itemResponses,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getDeliveryAddress(),
                order.getPaymentMethod().name(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
