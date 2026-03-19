package io.github.johneliud.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private BigDecimal subtotal;
}
