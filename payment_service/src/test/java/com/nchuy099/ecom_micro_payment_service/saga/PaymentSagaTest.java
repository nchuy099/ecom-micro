package com.nchuy099.ecom_micro_payment_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;

class PaymentSagaTest {
    @Test
    void completedPaymentIsRefundedOnCancellation() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var outbox = mock(SagaOutbox.class);
        when(jdbc.queryForObject(contains("SELECT state"), eq(String.class), eq("o1")))
                .thenReturn("COMPLETED", "REFUNDED");
        var saga = new PaymentSaga(jdbc, outbox);
        var event = new ObjectMapper().readTree("{\"orderId\":\"o1\"}");
        saga.cancel(event);
        saga.cancel(event);
        verify(jdbc, times(1)).update(contains("state='REFUNDED'"), eq("o1"));
        verifyNoInteractions(outbox);
    }

    @Test
    void cancellationBeforeStockEventPreventsPayment() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var outbox = mock(SagaOutbox.class);
        when(jdbc.queryForObject(contains("SELECT state"), eq(String.class), eq("o1")))
                .thenReturn("NEW", "CANCELLED");
        var saga = new PaymentSaga(jdbc, outbox);
        var mapper = new ObjectMapper();
        saga.cancel(mapper.readTree("{\"orderId\":\"o1\"}"));
        saga.pay(mapper.readTree("{\"orderId\":\"o1\",\"orderItems\":[]}"));
        verifyNoInteractions(outbox);
    }

    @Test
    void declinedPaymentPublishesFailure() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var outbox = mock(SagaOutbox.class);
        when(jdbc.queryForObject(contains("SELECT state"), eq(String.class), eq("o1")))
                .thenReturn("NEW");
        var saga = new PaymentSaga(jdbc, outbox);
        org.springframework.test.util.ReflectionTestUtils.setField(saga, "fail", true);
        saga.pay(new ObjectMapper().readTree("{\"orderId\":\"o1\",\"orderItems\":[]}"));
        verify(outbox).add(eq("payment-failed"), eq("o1"), any());
    }
}
