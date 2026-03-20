package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.dto.CheckoutRequest;
import io.github.johneliud.order_service.dto.OrderItemResponse;
import io.github.johneliud.order_service.dto.OrderResponse;
import io.github.johneliud.order_service.models.*;
import io.github.johneliud.order_service.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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
