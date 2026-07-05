package com.nchuy099.ecommerce.common;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        T payload
) {
    public static <T> EventEnvelope<T> create(String eventType, String aggregateType, String aggregateId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, aggregateType, aggregateId, Instant.now(), payload);
    }
}
