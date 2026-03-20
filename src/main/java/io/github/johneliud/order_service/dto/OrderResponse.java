package io.github.johneliud.order_service.dto;

import io.github.johneliud.order_service.models.DeliveryAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String userId;
    private String sellerId;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private String status;
    private DeliveryAddress deliveryAddress;
    private String paymentMethod;
    private Instant createdAt;
    private Instant updatedAt;
}
