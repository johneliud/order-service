package io.github.johneliud.order_service.repositories;

import io.github.johneliud.order_service.models.Order;
import io.github.johneliud.order_service.models.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findBySellerId(String sellerId);

    // Buyer paged queries
    Page<Order> findByUserId(String userId, Pageable pageable);
    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
    Page<Order> findByUserIdAndItemsProductNameContainingIgnoreCase(String userId, String search, Pageable pageable);
    Page<Order> findByUserIdAndStatusAndItemsProductNameContainingIgnoreCase(String userId, OrderStatus status, String search, Pageable pageable);

    // Seller paged queries
    Page<Order> findBySellerId(String sellerId, Pageable pageable);
    Page<Order> findBySellerIdAndStatus(String sellerId, OrderStatus status, Pageable pageable);
    Page<Order> findBySellerIdAndItemsProductNameContainingIgnoreCase(String sellerId, String search, Pageable pageable);
    Page<Order> findBySellerIdAndStatusAndItemsProductNameContainingIgnoreCase(String sellerId, OrderStatus status, String search, Pageable pageable);
}