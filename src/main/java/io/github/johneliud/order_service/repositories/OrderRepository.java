package io.github.johneliud.order_service.repositories;

import io.github.johneliud.order_service.models.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findBySellerId(String sellerId);
}
