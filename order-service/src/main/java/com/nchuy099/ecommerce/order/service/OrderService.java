package com.nchuy099.ecommerce.order.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nchuy099.ecommerce.common.event.OrderCancelledEvent;
import com.nchuy099.ecommerce.common.event.OrderCreatedEvent;
import com.nchuy099.ecommerce.common.event.OrderItemRequested;
import com.nchuy099.ecommerce.common.event.PaymentCompletedEvent;
import com.nchuy099.ecommerce.common.event.PaymentFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservationFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedItem;
import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.dto.PageResponse;
import com.nchuy099.ecommerce.order.entity.OrderEntity;
import com.nchuy099.ecommerce.order.entity.OrderItemEntity;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.OrderNotFoundException;
import com.nchuy099.ecommerce.order.exception.OrderStateConflictException;
import com.nchuy099.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    private final ProcessedEventService processedEventService;

    public OrderService(OrderRepository orderRepository, OutboxEventService outboxEventService, ProcessedEventService processedEventService) {
        this.orderRepository = orderRepository;
        this.outboxEventService = outboxEventService;
        this.processedEventService = processedEventService;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        OrderEntity order = new OrderEntity(orderNumber, request.userId(), OrderStatus.PENDING, BigDecimal.ZERO);

        for (OrderItemRequest item : request.items()) {
            order.addItem(OrderItemEntity.pending(order, item.productId(), item.quantity()));
        }

        OrderEntity savedOrder = orderRepository.saveAndFlush(order);
        List<OrderItemRequested> requestedItems = request.items().stream()
                .map(item -> new OrderItemRequested(item.productId(), item.quantity()))
                .toList();
        outboxEventService.writeOrderCreated(OrderCreatedEvent.of(savedOrder.getId(), savedOrder.getUserId(), requestedItems));

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findByUserId(Long userId, Pageable pageable) {
        Page<OrderEntity> page = orderRepository.findByUserId(userId, pageable);
        List<OrderResponse> content = page.getContent().stream()
                .map(OrderResponse::from)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderStateConflictException("Cannot cancel order with status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        OrderEntity updatedOrder = orderRepository.saveAndFlush(order);
        outboxEventService.writeOrderCancelled(OrderCancelledEvent.of(updatedOrder.getId(), updatedOrder.getUserId()));

        return OrderResponse.from(updatedOrder);
    }

    @Transactional
    public boolean applyStockReserved(StockReservedEvent event) {
        return processedEventService.processOnce(event.eventId(), event.eventType(), () -> {
            OrderEntity order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
            if (order.getStatus() != OrderStatus.PENDING) {
                log.info("Ignoring stock.reserved for order {} with status {}", order.getId(), order.getStatus());
                return true;
            }
            for (StockReservedItem item : event.items()) {
                order.findItem(item.productId()).applyReservedPrice(item.productName(), item.unitPrice(), item.subtotal());
            }
            order.setTotalAmount(event.totalAmount());
            orderRepository.saveAndFlush(order);
            return true;
        });
    }

    @Transactional
    public boolean applyPaymentCompleted(PaymentCompletedEvent event) {
        return processedEventService.processOnce(event.eventId(), event.eventType(), () -> {
            OrderEntity order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FAILED) {
                return true;
            }
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.saveAndFlush(order);
            return true;
        });
    }

    @Transactional
    public boolean applyPaymentFailed(PaymentFailedEvent event) {
        return processedEventService.processOnce(event.eventId(), event.eventType(), () -> {
            OrderEntity order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
            if (order.getStatus() == OrderStatus.CANCELLED) {
                return true;
            }
            order.setStatus(OrderStatus.CANCELLED);
            OrderEntity updatedOrder = orderRepository.saveAndFlush(order);
            outboxEventService.writeOrderCancelled(OrderCancelledEvent.of(updatedOrder.getId(), updatedOrder.getUserId()));
            return true;
        });
    }

    @Transactional
    public boolean applyStockReservationFailed(StockReservationFailedEvent event) {
        return processedEventService.processOnce(event.eventId(), event.eventType(), () -> {
            OrderEntity order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.saveAndFlush(order);
            }
            return true;
        });
    }
}
