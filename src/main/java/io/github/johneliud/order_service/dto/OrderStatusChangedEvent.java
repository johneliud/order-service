package io.github.johneliud.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusChangedEvent {
    private String orderId;
    private String userId;
    private String sellerId;
    private String oldStatus;
    private String newStatus;
    private List<OrderItemEvent> items;
}
