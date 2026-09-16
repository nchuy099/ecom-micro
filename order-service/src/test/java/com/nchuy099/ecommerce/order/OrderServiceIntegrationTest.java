package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.client.ProductClient;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.dto.PageResponse;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.repository.OrderRepository;
import com.nchuy099.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "eureka.client.enabled=false"
})
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductClient productClient;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        when(productClient.reserveStockForOrder(anyLong(), anyLong(), anyLong(), anyInt()))
                .thenAnswer(invocation -> {
                    Long productId = invocation.getArgument(2);
                    Integer quantity = invocation.getArgument(3);
                    return new ProductClientResponse(
                            productId,
                            "Product " + productId,
                            "SKU-" + productId,
                            new BigDecimal("500.00"),
                            100 - quantity,
                            null,
                            1L
                    );
                });
    }

    @Test
    void createsConfirmedOrderAfterSynchronousStockReservation() {
        OrderResponse order = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 2), new OrderItemRequest(20L, 1))
        ));

        assertThat(order.id()).isNotNull();
        assertThat(order.orderNumber()).startsWith("ORD-");
        assertThat(order.userId()).isEqualTo(100L);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.totalAmount()).isEqualByComparingTo("1500.00");
        assertThat(order.items()).hasSize(2);
        assertThat(order.items()).extracting(com.nchuy099.ecommerce.order.dto.OrderItemResponse::productName)
                .containsExactly("Product 10", "Product 20");

    }

    @Test
    void synchronousReservationReturnsProductPricingImmediately() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 1))
        ));

        OrderResponse priced = orderService.findById(created.id());
        assertThat(priced.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(priced.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(priced.items().get(0).productName()).isEqualTo("Product 10");
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
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listsOrdersWithKeysetCursorWithoutDuplicates() {
        OrderResponse first = orderService.create(new CreateOrderRequest(300L, List.of(new OrderItemRequest(10L, 1))));
        OrderResponse second = orderService.create(new CreateOrderRequest(300L, List.of(new OrderItemRequest(10L, 1))));
        OrderResponse third = orderService.create(new CreateOrderRequest(300L, List.of(new OrderItemRequest(10L, 1))));

        PageResponse<OrderResponse> firstPage = orderService.findByUserIdCursor(300L, null, 2);
        PageResponse<OrderResponse> secondPage = orderService.findByUserIdCursor(300L, firstPage.nextCursor(), 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(firstPage.content()).extracting(OrderResponse::id)
                .doesNotContain(secondPage.content().get(0).id());
        assertThat(firstPage.content()).extracting(OrderResponse::id)
                .containsExactly(third.id(), second.id());
        assertThat(secondPage.content()).extracting(OrderResponse::id)
                .containsExactly(first.id());
    }

    @Test
    void manualCancelConfirmedOrderReleasesStockSynchronously() {
        OrderResponse created = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(10L, 1))
        ));
        OrderResponse cancelled = orderService.cancel(created.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        org.mockito.Mockito.verify(productClient).releaseOrderStock(created.id());
        assertThatThrownBy(() -> orderService.cancel(created.id()))
                .isInstanceOf(BusinessException.class);
    }
}
