package com.nchuy099.ecom_micro_payment_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_payment_service.saga.PaymentSaga;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReservedConsumer {
    private final PaymentSaga payment;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "stock-reserved")
    public void pay(String json) throws Exception {
        payment.pay(mapper.readTree(json));
    }

    @KafkaListener(topics = "order-cancelled")
    public void cancel(String json) throws Exception {
        payment.cancel(mapper.readTree(json));
    }
}
