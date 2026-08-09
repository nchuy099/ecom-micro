package com.nchuy099.ecommerce.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.common.event.OrderCancelledEvent;
import com.nchuy099.ecommerce.common.event.OrderCreatedEvent;
import com.nchuy099.ecommerce.common.event.PaymentCompletedEvent;
import com.nchuy099.ecommerce.common.event.PaymentFailedEvent;
import com.nchuy099.ecommerce.order.entity.OutboxEventEntity;
import com.nchuy099.ecommerce.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventService {
    private static final String ORDER_AGGREGATE_TYPE = "order";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public OutboxEventEntity writeOrderCreated(OrderCreatedEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.createdAt());
    }

    public OutboxEventEntity writeOrderCancelled(OrderCancelledEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.cancelledAt());
    }

    public OutboxEventEntity writePaymentCompleted(PaymentCompletedEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.completedAt());
    }

    public OutboxEventEntity writePaymentFailed(PaymentFailedEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.failedAt());
    }

    private OutboxEventEntity save(java.util.UUID eventId, String aggregateId, String eventType, Object payload, java.time.Instant occurredAt) {
        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                eventId,
                ORDER_AGGREGATE_TYPE,
                aggregateId,
                eventType,
                toJson(payload),
                occurredAt
        );
        return outboxEventRepository.save(outboxEvent);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox event payload", ex);
        }
    }
}
