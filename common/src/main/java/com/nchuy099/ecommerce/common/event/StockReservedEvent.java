package com.nchuy099.ecommerce.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        Long orderId,
        Long userId,
        List<StockReservedItem> items,
        BigDecimal totalAmount,
        Instant reservedAt
) {
    public static StockReservedEvent of(Long orderId, Long userId, List<StockReservedItem> items) {
        BigDecimal totalAmount = items.stream()
                .map(StockReservedItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockReservedEvent(
                UUID.randomUUID(),
                KafkaTopics.STOCK_RESERVED,
                String.valueOf(orderId),
                orderId,
                userId,
                List.copyOf(items),
                totalAmount,
                Instant.now()
        );
    }
}
