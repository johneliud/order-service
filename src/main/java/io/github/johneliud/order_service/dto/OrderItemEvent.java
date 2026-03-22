package io.github.johneliud.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {
    private String productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
}
