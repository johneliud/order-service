package io.github.johneliud.order_service.services;

import io.github.johneliud.order_service.dto.OrderPlacedEvent;
import io.github.johneliud.order_service.dto.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String ORDER_PLACED_TOPIC = "order-placed";
    private static final String ORDER_STATUS_CHANGED_TOPIC = "order-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(ORDER_PLACED_TOPIC, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order-placed event for orderId {}: {}", event.getOrderId(), ex.getMessage());
                    } else {
                        log.info("Published order-placed event for orderId {} to partition {} offset {}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        kafkaTemplate.send(ORDER_STATUS_CHANGED_TOPIC, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order-status-changed event for orderId {}: {}", event.getOrderId(), ex.getMessage());
                    } else {
                        log.info("Published order-status-changed event for orderId {} ({} → {}) to partition {} offset {}",
                                event.getOrderId(),
                                event.getOldStatus(),
                                event.getNewStatus(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
