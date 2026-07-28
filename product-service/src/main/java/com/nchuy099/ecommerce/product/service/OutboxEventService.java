package com.nchuy099.ecommerce.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.common.event.StockReleasedEvent;
import com.nchuy099.ecommerce.common.event.StockReservationFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.product.entity.OutboxEventEntity;
import com.nchuy099.ecommerce.product.repository.OutboxEventRepository;
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

    public OutboxEventEntity writeStockReserved(StockReservedEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.reservedAt());
    }

    public OutboxEventEntity writeStockReservationFailed(StockReservationFailedEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.failedAt());
    }

    public OutboxEventEntity writeStockReleased(StockReleasedEvent event) {
        return save(event.eventId(), event.aggregateId(), event.eventType(), event, event.releasedAt());
    }

    private OutboxEventEntity save(java.util.UUID eventId, String aggregateId, String eventType, Object payload, java.time.Instant occurredAt) {
        return outboxEventRepository.save(new OutboxEventEntity(
                eventId,
                ORDER_AGGREGATE_TYPE,
                aggregateId,
                eventType,
                toJson(payload),
                occurredAt
        ));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox event payload", ex);
        }
    }
}
