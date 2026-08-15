package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.common.event.KafkaTopics;
import com.nchuy099.ecommerce.common.event.PaymentCompletedEvent;
import com.nchuy099.ecommerce.common.event.PaymentFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservationFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedItem;
import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.dto.PageResponse;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.OrderNotFoundException;
import com.nchuy099.ecommerce.order.exception.OrderStateConflictException;
import com.nchuy099.ecommerce.order.repository.OutboxEventRepository;
import com.nchuy099.ecommerce.order.repository.OrderRepository;
import com.nchuy099.ecommerce.order.repository.ProcessedEventRepository;
import com.nchuy099.ecommerce.order.listener.PaymentMockListener;
import com.nchuy099.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "ecommerce.kafka.topics.enabled=false",
        "saga.payment.mock.failure-user-ids=999"
})
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentMockListener paymentMockListener;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void createsPendingOrderAndWritesOrderCreatedOutbox() throws Exception {
        OrderResponse order = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 2), new OrderItemRequest(20L, 1))
        ));

        assertThat(order.id()).isNotNull();
        assertThat(order.orderNumber()).startsWith("ORD-");
        assertThat(order.userId()).isEqualTo(100L);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.items()).hasSize(2);

        List<com.nchuy099.ecommerce.order.entity.OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        com.nchuy099.ecommerce.order.entity.OutboxEventEntity outboxEvent = outboxEvents.get(0);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("order");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(String.valueOf(order.id()));
        assertThat(outboxEvent.getType()).isEqualTo(KafkaTopics.ORDER_CREATED);

        JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(payload.get("eventId").asText()).isEqualTo(outboxEvent.getId());
        assertThat(payload.get("eventType").asText()).isEqualTo(KafkaTopics.ORDER_CREATED);
        assertThat(payload.get("aggregateId").asText()).isEqualTo(String.valueOf(order.id()));
        assertThat(payload.get("orderId").asLong()).isEqualTo(order.id());
        assertThat(payload.get("userId").asLong()).isEqualTo(100L);
        assertThat(payload.get("items")).hasSize(2);
        assertThat(payload.get("createdAt").asText()).isNotBlank();
    }

    @Test
    void stockReservedUpdatesOrderAndPaymentCompletedConfirmsIt() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 1))
        ));

        orderService.applyStockReserved(StockReservedEvent.of(
                created.id(),
                100L,
                List.of(new StockReservedItem(10L, "Phone", 1, new BigDecimal("500.00"), new BigDecimal("500.00")))
        ));
        OrderResponse priced = orderService.findById(created.id());
        assertThat(priced.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(priced.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(priced.items().get(0).productName()).isEqualTo("Phone");

        orderService.applyPaymentCompleted(PaymentCompletedEvent.of(created.id(), 100L));
        assertThat(orderService.findById(created.id()).status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void paymentFailureCancelsOrderAndEmitsOrderCancelled() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 2))
        ));

        orderService.applyPaymentFailed(PaymentFailedEvent.of(created.id(), 100L, "Mock payment failed"));

        assertThat(orderService.findById(created.id()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(outboxEventRepository.findAll())
                .extracting(com.nchuy099.ecommerce.order.entity.OutboxEventEntity::getType)
                .contains(KafkaTopics.ORDER_CREATED, KafkaTopics.ORDER_CANCELLED);
    }

    @Test
    void duplicatePaymentFailureDoesNotEmitDuplicateOrderCancelled() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 2))
        ));
        PaymentFailedEvent event = PaymentFailedEvent.of(created.id(), 100L, "Mock payment failed");

        orderService.applyPaymentFailed(event);
        orderService.applyPaymentFailed(event);

        assertThat(outboxEventRepository.findAll())
                .extracting(com.nchuy099.ecommerce.order.entity.OutboxEventEntity::getType)
                .containsExactly(KafkaTopics.ORDER_CREATED, KafkaTopics.ORDER_CANCELLED);
    }

    @Test
    void stockReservationFailureMarksOrderFailed() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 2))
        ));

        orderService.applyStockReservationFailed(StockReservationFailedEvent.of(created.id(), 100L, "Insufficient stock"));

        assertThat(orderService.findById(created.id()).status()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void paymentMockEmitsCompletedOrFailedOnce() {
        StockReservedEvent successEvent = StockReservedEvent.of(
                1000L,
                100L,
                List.of(new StockReservedItem(10L, "Phone", 1, new BigDecimal("500.00"), new BigDecimal("500.00")))
        );
        StockReservedEvent failureEvent = StockReservedEvent.of(
                2000L,
                999L,
                List.of(new StockReservedItem(10L, "Phone", 1, new BigDecimal("500.00"), new BigDecimal("500.00")))
        );

        paymentMockListener.handleStockReserved(successEvent);
        paymentMockListener.handleStockReserved(successEvent);
        paymentMockListener.handleStockReserved(failureEvent);

        assertThat(outboxEventRepository.findAll())
                .extracting(com.nchuy099.ecommerce.order.entity.OutboxEventEntity::getType)
                .containsExactly(KafkaTopics.PAYMENT_COMPLETED, KafkaTopics.PAYMENT_FAILED);
    }

    @Test
    void findsOrderByIdAndListsByUserWithPagination() {
        OrderResponse order1 = orderService.create(new CreateOrderRequest(100L, List.of(new OrderItemRequest(10L, 1))));
        OrderResponse order2 = orderService.create(new CreateOrderRequest(100L, List.of(new OrderItemRequest(10L, 1))));
        orderService.create(new CreateOrderRequest(200L, List.of(new OrderItemRequest(10L, 1))));

        OrderResponse found = orderService.findById(order1.id());
        assertThat(found.orderNumber()).isEqualTo(order1.orderNumber());

        PageResponse<OrderResponse> page = orderService.findByUserId(100L, PageRequest.of(0, 10));
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(OrderResponse::id).containsExactlyInAnyOrder(order1.id(), order2.id());

        assertThatThrownBy(() -> orderService.findById(999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void manualCancelConfirmedOrderEmitsOrderCancelled() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 1))
        ));
        orderService.applyPaymentCompleted(PaymentCompletedEvent.of(created.id(), 100L));

        OrderResponse cancelled = orderService.cancel(created.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> orderService.cancel(created.id()))
                .isInstanceOf(OrderStateConflictException.class);
        assertThat(outboxEventRepository.findAll())
                .extracting(com.nchuy099.ecommerce.order.entity.OutboxEventEntity::getType)
                .contains(KafkaTopics.ORDER_CANCELLED);
    }
}
