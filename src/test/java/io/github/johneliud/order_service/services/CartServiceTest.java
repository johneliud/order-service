package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.client.ProductServiceClient;
import io.github.johneliud.order_service.dto.*;
import io.github.johneliud.order_service.models.Cart;
import io.github.johneliud.order_service.models.CartItem;
import io.github.johneliud.order_service.repositories.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartService cartService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private CartItem item(String productId, String sellerId, int qty) {
        return new CartItem(productId, "Product " + productId, new BigDecimal("10.00"), qty, null, sellerId);
    }

    private Cart cartWith(String userId, List<CartItem> items) {
        Cart c = new Cart();
        c.setId("cart1");
        c.setUserId(userId);
        c.setItems(new ArrayList<>(items));
        return c;
    }

    private Cart emptyCart(String userId) {
        return cartWith(userId, new ArrayList<>());
    }

    private CheckoutRequest checkoutRequest() {
        DeliveryAddressRequest addr = new DeliveryAddressRequest("Name", "123 St", "City", "0700000000");
        return new CheckoutRequest(addr, "PAY_ON_DELIVERY");
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    void addItem_newItem_addsToCart() {
        Cart cart = emptyCart("user1");
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productServiceClient.getProduct("p1"))
                .thenReturn(new ProductDto("p1", "Product p1", new BigDecimal("10.00"), 10, "seller1"));

        CartItemRequest req = new CartItemRequest("p1", 2, null);
        CartResponse result = cartService.addItem("user1", req);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductId()).isEqualTo("p1");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_duplicateItem_incrementsQuantity() {
        Cart cart = cartWith("user1", List.of(item("p1", "seller1", 2)));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productServiceClient.getProduct("p1"))
                .thenReturn(new ProductDto("p1", "Product p1", new BigDecimal("10.00"), 10, "seller1"));

        CartItemRequest req = new CartItemRequest("p1", 1, null);
        CartResponse result = cartService.addItem("user1", req);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    // ── updateItem ────────────────────────────────────────────────────────────

    @Test
    void updateItem_updatesQuantity() {
        Cart cart = cartWith("user1", List.of(item("p1", "seller1", 2)));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse result = cartService.updateItem("user1", "p1", new UpdateCartItemRequest(5));

        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItem_itemNotFound_throws() {
        Cart cart = emptyCart("user1");
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem("user1", "p-missing", new UpdateCartItemRequest(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item not found in cart");
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Test
    void removeItem_removesFromCart() {
        Cart cart = cartWith("user1", List.of(item("p1", "seller1", 1)));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse result = cartService.removeItem("user1", "p1");

        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void removeItem_itemNotFound_throws() {
        Cart cart = emptyCart("user1");
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem("user1", "p-missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item not found in cart");
    }

    // ── clearCart ─────────────────────────────────────────────────────────────

    @Test
    void clearCart_clearsAllItems() {
        Cart cart = cartWith("user1", List.of(item("p1", "s1", 1), item("p2", "s1", 2)));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        cartService.clearCart("user1");

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().getItems()).isEmpty();
    }

    // ── checkout ──────────────────────────────────────────────────────────────

    @Test
    void checkout_validCart_createsOrderAndClearsCart() {
        Cart cart = cartWith("user1", List.of(item("p1", "seller1", 1), item("p2", "seller1", 2)));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(orderService.createOrder(any(), any(), any(), any())).thenReturn(new OrderResponse());
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productServiceClient.getProduct("p1"))
                .thenReturn(new ProductDto("p1", "Product p1", new BigDecimal("10.00"), 10, "seller1"));
        when(productServiceClient.getProduct("p2"))
                .thenReturn(new ProductDto("p2", "Product p2", new BigDecimal("10.00"), 10, "seller1"));

        List<OrderResponse> orders = cartService.checkout("user1", checkoutRequest());

        assertThat(orders).hasSize(1);
        verify(orderService).createOrder(eq("user1"), eq("seller1"), any(), any());
        verify(productServiceClient).decrementStock("p1", 1);
        verify(productServiceClient).decrementStock("p2", 2);
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(c -> c.getItems().isEmpty())).isTrue();
    }

    @Test
    void checkout_insufficientStock_throws() {
        Cart cart = cartWith("user1", List.of(item("p1", "seller1", 5)));
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));
        when(productServiceClient.getProduct("p1"))
                .thenReturn(new ProductDto("p1", "Product p1", new BigDecimal("10.00"), 2, "seller1"));

        assertThatThrownBy(() -> cartService.checkout("user1", checkoutRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void checkout_emptyCart_throws() {
        Cart cart = emptyCart("user1");
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.checkout("user1", checkoutRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot checkout with an empty cart");
    }
}
