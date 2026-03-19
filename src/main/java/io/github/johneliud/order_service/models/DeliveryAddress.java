package io.github.johneliud.order_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddress {
    private String fullName;
    private String address;
    private String city;
    private String phone;
}
