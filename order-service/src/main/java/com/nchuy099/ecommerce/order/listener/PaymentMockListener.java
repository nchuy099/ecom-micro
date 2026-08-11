package com.nchuy099.ecommerce.order.listener;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.nchuy099.ecommerce.common.event.KafkaTopics;
import com.nchuy099.ecommerce.common.event.PaymentCompletedEvent;
import com.nchuy099.ecommerce.common.event.PaymentFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.order.service.OutboxEventService;
import com.nchuy099.ecommerce.order.service.ProcessedEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentMockListener {
    private final OutboxEventService outboxEventService;
    private final ProcessedEventService processedEventService;
    private final Set<Long> failureUserIds;

    public PaymentMockListener(
            OutboxEventService outboxEventService,
            ProcessedEventService processedEventService,
            @Value("${saga.payment.mock.failure-user-ids:}") String failureUserIds
    ) {
        this.outboxEventService = outboxEventService;
        this.processedEventService = processedEventService;
        this.failureUserIds = parseFailureUserIds(failureUserIds);
    }

    @Transactional
    @KafkaListener(topics = KafkaTopics.STOCK_RESERVED, groupId = "payment-mock-saga")
    public void handleStockReserved(StockReservedEvent event) {
        processedEventService.processOnce(event.eventId(), "payment-mock:" + event.eventType(), () -> {
            if (failureUserIds.contains(event.userId())) {
                outboxEventService.writePaymentFailed(PaymentFailedEvent.of(event.orderId(), event.userId(), "Mock payment failed"));
            } else {
                outboxEventService.writePaymentCompleted(PaymentCompletedEvent.of(event.orderId(), event.userId()));
            }
            return true;
        });
    }

    private static Set<Long> parseFailureUserIds(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}
