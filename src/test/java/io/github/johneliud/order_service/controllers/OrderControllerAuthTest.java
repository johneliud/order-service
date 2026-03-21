package io.github.johneliud.order_service.controllers;

import io.github.johneliud.order_service.dto.UpdateOrderStatusRequest;
import io.github.johneliud.order_service.exception.ForbiddenException;
import io.github.johneliud.order_service.exception.UnauthorizedException;
import io.github.johneliud.order_service.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderControllerAuthTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getMyOrders_missingHeaders_throws401() {
        assertThatThrownBy(() -> orderController.getMyOrders(0, 10, null, null, null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getMyOrders_sellerRole_throws403() {
        assertThatThrownBy(() -> orderController.getMyOrders(0, 10, null, null, "user1", "SELLER"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getSellerOrders_missingHeaders_throws401() {
        assertThatThrownBy(() -> orderController.getSellerOrders(0, 10, null, null, null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getSellerOrders_clientRole_throws403() {
        assertThatThrownBy(() -> orderController.getSellerOrders(0, 10, null, null, "user1", "CLIENT"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelOrder_missingHeaders_throws401() {
        assertThatThrownBy(() -> orderController.cancelOrder("o1", null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateOrderStatus_clientRole_throws403() {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest("CONFIRMED");
        assertThatThrownBy(() -> orderController.updateOrderStatus("o1", req, "user1", "CLIENT"))
                .isInstanceOf(ForbiddenException.class);
    }
}
