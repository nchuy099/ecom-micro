package com.nchuy099.ecom_micro_product_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import com.nchuy099.ecom_micro_product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class StockSagaTest {
    @Test
    void cancellationReleasesReservedStock() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var repo = mock(ProductRepository.class);
        var outbox = mock(SagaOutbox.class);
        when(jdbc.queryForObject(contains("SELECT state"), eq(String.class), eq("o1")))
                .thenReturn("RESERVED", "RELEASED");
        when(jdbc.queryForObject(contains("SELECT items"), eq(String.class), eq("o1")))
                .thenReturn("[{\"productId\":\"p1\",\"quantity\":2}]");
        var product = new ProductEntity();
        product.setId("p1");
        product.setStock(3);
        when(repo.findByIdInForUpdate(anyList())).thenReturn(List.of(product));
        var saga = new StockSaga(jdbc, repo, outbox, new ObjectMapper(), mock(CacheManager.class));
        var event = new ObjectMapper().readTree("{\"orderId\":\"o1\"}");
        saga.cancel(event);
        saga.cancel(event);
        assertEquals(5, product.getStock());
        verify(outbox, times(1)).add(eq("stock-released"), eq("o1"), any());
    }

    @Test
    void cancellationBeforeCreationPreventsStockReservation() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var repo = mock(ProductRepository.class);
        var outbox = mock(SagaOutbox.class);
        when(jdbc.queryForObject(contains("SELECT state"), eq(String.class), eq("o1")))
                .thenReturn("NEW", "CANCELLED");
        var saga = new StockSaga(jdbc, repo, outbox, new ObjectMapper(), mock(CacheManager.class));
        var mapper = new ObjectMapper();
        saga.cancel(mapper.readTree("{\"orderId\":\"o1\"}"));
        saga.reserve(
                mapper.readTree(
                        "{\"id\":\"o1\",\"orderItems\":[{\"productId\":\"p1\",\"quantity\":1}]}"));
        verifyNoInteractions(repo, outbox);
    }

    @Test
    void insufficientStockPublishesFailureWithoutDeduction() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var repo = mock(ProductRepository.class);
        var outbox = mock(SagaOutbox.class);
        when(jdbc.queryForObject(contains("SELECT state"), eq(String.class), eq("o1")))
                .thenReturn("NEW");
        var product = new ProductEntity();
        product.setId("p1");
        product.setStock(1);
        when(repo.findByIdInForUpdate(anyList())).thenReturn(List.of(product));
        var mapper = new ObjectMapper();
        var saga = new StockSaga(jdbc, repo, outbox, mapper, mock(CacheManager.class));
        saga.reserve(
                mapper.readTree(
                        "{\"id\":\"o1\",\"orderItems\":[{\"productId\":\"p1\",\"quantity\":2}]}"));
        assertEquals(1, product.getStock());
        verify(outbox).add(eq("stock-reservation-failed"), eq("o1"), any());
        verify(repo, never()).saveAll(any());
    }
}
