package com.nchuy099.ecommerce.order.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.dto.PageResponse;
import com.nchuy099.ecommerce.order.client.ProductClient;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.entity.OrderEntity;
import com.nchuy099.ecommerce.order.entity.OrderItemEntity;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.repository.OrderRepository;
import com.nchuy099.ecommerce.order.pagination.OrderCursor;
import com.nchuy099.ecommerce.order.notification.OrderNotificationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderNotificationPublisher notificationPublisher;

    @Autowired
    public OrderService(OrderRepository orderRepository, ProductClient productClient,
                        OrderNotificationPublisher notificationPublisher) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.notificationPublisher = notificationPublisher;
    }

    // Keeps focused unit tests independent from Kafka while Spring uses the producer-backed constructor.
    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this(orderRepository, productClient, OrderNotificationPublisher.noop());
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        validateDistinctProducts(request);
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        OrderEntity order = new OrderEntity(orderNumber, request.userId(), OrderStatus.PENDING, BigDecimal.ZERO);

        for (OrderItemRequest item : request.items()) {
            order.addItem(OrderItemEntity.pending(order, item.productId(), item.quantity()));
        }

        OrderEntity savedOrder = orderRepository.saveAndFlush(order);
        try {
            BigDecimal total = BigDecimal.ZERO;
            List<OrderItemRequest> itemsByProduct = request.items().stream()
                    .sorted(java.util.Comparator.comparing(OrderItemRequest::productId))
                    .toList();

            for (OrderItemRequest item : itemsByProduct) {
                ProductClientResponse product = productClient.reserveStockForOrder(
                        savedOrder.getId(),
                        savedOrder.getUserId(),
                        item.productId(),
                        item.quantity()
                );
                BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(item.quantity()));
                savedOrder.findItem(item.productId()).applyReservedPrice(product.name(), product.price(), subtotal);
                total = total.add(subtotal);
            }

            savedOrder.setTotalAmount(total);
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            OrderEntity confirmedOrder = orderRepository.saveAndFlush(savedOrder);
            notificationPublisher.publishOrderConfirmed(
                    confirmedOrder.getId(),
                    confirmedOrder.getUserId(),
                    confirmedOrder.getOrderNumber(),
                    "Your order " + confirmedOrder.getOrderNumber() + " has been confirmed."
            );
            return OrderResponse.from(confirmedOrder);
        } catch (RuntimeException ex) {
            try {
                productClient.releaseOrderStock(savedOrder.getId());
            } catch (RuntimeException compensationFailure) {
                log.error("Failed to release product reservations for order {} after create failure", savedOrder.getId(), compensationFailure);
                ex.addSuppressed(compensationFailure);
            }
            throw ex;
        }
    }

    private static void validateDistinctProducts(CreateOrderRequest request) {
        long uniqueProductCount = request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .count();
        if (uniqueProductCount != request.items().size()) {
            throw BusinessException.badRequest(
                    "https://errors.ecom.local/invalid-pagination",
                    "Invalid pagination request",
                    "Order cannot contain duplicate products"
            );
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> orderNotFound(id));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findByUserId(Long userId, Pageable pageable) {
        Page<Long> page = orderRepository.findIdsByUserId(userId, pageable);
        List<OrderResponse> content = loadInRequestedOrder(page.getContent()).stream()
                .map(OrderResponse::from)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findByUserIdCursor(Long userId, String cursor, int size) {
        int requestedSize = Math.min(Math.max(size, 1), 100);
        List<Long> ids;
        if (cursor == null || cursor.isBlank()) {
            ids = orderRepository.findIdsByUserIdOrderByCreatedAtDesc(
                    userId,
                    PageRequest.of(0, requestedSize + 1)
            );
        } else {
            OrderCursor decoded = OrderCursor.decode(cursor);
            ids = orderRepository.findIdsByUserIdAfterCursor(
                    userId,
                    decoded.createdAt(),
                    decoded.id(),
                    PageRequest.of(0, requestedSize + 1)
            );
        }

        boolean hasNext = ids.size() > requestedSize;
        List<Long> pageIds = ids.stream().limit(requestedSize).toList();
        List<OrderEntity> entities = loadInRequestedOrder(pageIds);
        String nextCursor = hasNext && !entities.isEmpty()
                ? OrderCursor.encode(entities.get(entities.size() - 1).getCreatedAt(), entities.get(entities.size() - 1).getId())
                : null;
        return PageResponse.cursor(
                entities.stream().map(OrderResponse::from).toList(),
                requestedSize,
                nextCursor,
                hasNext
        );
    }

    private List<OrderEntity> loadInRequestedOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, OrderEntity> byId = orderRepository.findAllWithItemsByIdIn(ids).stream()
                .collect(Collectors.toMap(OrderEntity::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> orderNotFound(id));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw BusinessException.conflict(
                    "https://errors.ecom.local/order-conflict",
                    "Order state conflict",
                    "Cannot cancel order with status " + order.getStatus()
            );
        }

        productClient.releaseOrderStock(id);
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    private static BusinessException orderNotFound(Long id) {
        return BusinessException.notFound(
                "https://errors.ecom.local/order-not-found",
                "Order not found",
                "Order not found with id: " + id
        );
    }
}
