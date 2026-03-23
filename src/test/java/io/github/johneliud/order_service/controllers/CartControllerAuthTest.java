package io.github.johneliud.order_service.controllers;

import io.github.johneliud.order_service.dto.CartItemRequest;
import io.github.johneliud.order_service.dto.CheckoutRequest;
import io.github.johneliud.order_service.dto.DeliveryAddressRequest;
import io.github.johneliud.order_service.exception.ForbiddenException;
import io.github.johneliud.order_service.exception.UnauthorizedException;
import io.github.johneliud.order_service.services.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CartControllerAuthTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @Test
    void getCart_missingHeaders_throws401() {
        assertThatThrownBy(() -> cartController.getCart(null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCart_wrongRole_throws403() {
        assertThatThrownBy(() -> cartController.getCart("user1", "SELLER"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void addItem_missingHeaders_throws401() {
        CartItemRequest req = new CartItemRequest("p1", 1, null);
        assertThatThrownBy(() -> cartController.addItem(req, null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void checkout_wrongRole_throws403() {
        CheckoutRequest req = new CheckoutRequest(
                new DeliveryAddressRequest("Name", "Addr", "City", "0700"), "PAY_ON_DELIVERY");
        assertThatThrownBy(() -> cartController.checkout(req, "user1", "SELLER"))
                .isInstanceOf(ForbiddenException.class);
    }
}
