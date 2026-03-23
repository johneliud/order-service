package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.dto.PagedResponse;
import io.github.johneliud.order_service.exception.ForbiddenException;
import io.github.johneliud.order_service.models.*;
import io.github.johneliud.order_service.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Order order(String id, String userId, String sellerId, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setSellerId(sellerId);
        o.setStatus(status);
        o.setItems(List.of());
        o.setTotalAmount(BigDecimal.TEN);
        o.setPaymentMethod(PaymentMethod.PAY_ON_DELIVERY);
        o.setDeliveryAddress(new DeliveryAddress("Name", "Addr", "City", "0700000000"));
        return o;
    }

    private Page<Order> pageOf(Order... orders) {
        return new PageImpl<>(List.of(orders), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), orders.length);
    }

    // ── getOrdersByBuyer ──────────────────────────────────────────────────────

    @Test
    void getOrdersByBuyer_noFilters_returnsPaged() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(pageOf(o));

        PagedResponse<?> result = orderService.getOrdersByBuyer("u1", 0, 10, null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByUserId(eq("u1"), any(Pageable.class));
    }

    @Test
    void getOrdersByBuyer_withStatus_usesStatusQuery() {
        when(orderRepository.findByUserIdAndStatus(eq("u1"), eq(OrderStatus.PENDING), any())).thenReturn(pageOf());

        orderService.getOrdersByBuyer("u1", 0, 10, null, "PENDING");

        verify(orderRepository).findByUserIdAndStatus(eq("u1"), eq(OrderStatus.PENDING), any());
    }

    @Test
    void getOrdersByBuyer_withSearch_usesSearchQuery() {
        when(orderRepository.findByUserIdAndItemsProductNameContainingIgnoreCase(eq("u1"), eq("phone"), any())).thenReturn(pageOf());

        orderService.getOrdersByBuyer("u1", 0, 10, "phone", null);

        verify(orderRepository).findByUserIdAndItemsProductNameContainingIgnoreCase(eq("u1"), eq("phone"), any());
    }

    // ── getOrdersBySeller ─────────────────────────────────────────────────────

    @Test
    void getOrdersBySeller_noFilters_returnsPaged() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findBySellerId(eq("s1"), any(Pageable.class))).thenReturn(pageOf(o));

        PagedResponse<?> result = orderService.getOrdersBySeller("s1", 0, 10, null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findBySellerId(eq("s1"), any(Pageable.class));
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    void cancelOrder_pendingOrder_setsStatusCancelled() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder("o1", "u1");

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(o);
    }

    @Test
    void cancelOrder_nonPendingOrder_throws() {
        Order o = order("o1", "u1", "s1", OrderStatus.CONFIRMED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.cancelOrder("o1", "u1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void cancelOrder_wrongUser_throws403() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.cancelOrder("o1", "other-user"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── removeOrder ───────────────────────────────────────────────────────────

    @Test
    void removeOrder_cancelledOrder_deletes() {
        Order o = order("o1", "u1", "s1", OrderStatus.CANCELLED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        orderService.removeOrder("o1", "u1");

        verify(orderRepository).delete(o);
    }

    @Test
    void removeOrder_deliveredOrder_deletes() {
        Order o = order("o1", "u1", "s1", OrderStatus.DELIVERED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        orderService.removeOrder("o1", "u1");

        verify(orderRepository).delete(o);
    }

    @Test
    void removeOrder_activeOrder_throws() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.removeOrder("o1", "u1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANCELLED or DELIVERED");
    }

    @Test
    void removeOrder_wrongUser_throws403() {
        Order o = order("o1", "u1", "s1", OrderStatus.CANCELLED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.removeOrder("o1", "other-user"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── updateOrderStatus ─────────────────────────────────────────────────────

    @Test
    void updateOrderStatus_pendingToConfirmed_succeeds() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(orderEventPublisher).publishOrderStatusChanged(any());

        orderService.updateOrderStatus("o1", "s1", "CONFIRMED");

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderEventPublisher).publishOrderStatusChanged(any());
    }

    @Test
    void updateOrderStatus_sellerCancelsPendingOrder_succeeds() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(orderEventPublisher).publishOrderStatusChanged(any());

        orderService.updateOrderStatus("o1", "s1", "CANCELLED");

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPublisher).publishOrderStatusChanged(any());
    }

    @Test
    void updateOrderStatus_sellerCancelsConfirmedOrder_succeeds() {
        Order o = order("o1", "u1", "s1", OrderStatus.CONFIRMED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(orderEventPublisher).publishOrderStatusChanged(any());

        orderService.updateOrderStatus("o1", "s1", "CANCELLED");

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPublisher).publishOrderStatusChanged(any());
    }

    @Test
    void updateOrderStatus_invalidTransition_throws() {
        Order o = order("o1", "u1", "s1", OrderStatus.CONFIRMED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.updateOrderStatus("o1", "s1", "DELIVERED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateOrderStatus_wrongSeller_throws403() {
        Order o = order("o1", "u1", "s1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.updateOrderStatus("o1", "other-seller", "CONFIRMED"))
                .isInstanceOf(ForbiddenException.class);
    }
}
