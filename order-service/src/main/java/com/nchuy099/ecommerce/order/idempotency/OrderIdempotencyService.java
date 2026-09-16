package com.nchuy099.ecommerce.order.idempotency;

import java.time.Duration;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.order.api.ApiResponse;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderIdempotencyService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration completedTtl;
    private final Duration processingTtl;
    private final JavaType recordType;

    public OrderIdempotencyService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${idempotency.order-create.completed-ttl:24h}") Duration completedTtl,
            @Value("${idempotency.order-create.processing-ttl:60s}") Duration processingTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.completedTtl = completedTtl;
        this.processingTtl = processingTtl;
        this.recordType = objectMapper.getTypeFactory().constructType(IdempotencyRecord.class);
    }

    public ApiResponse<OrderResponse> executeCreateOrder(
            String callerId,
            String idempotencyKey,
            Supplier<OrderResponse> action
    ) {
        String key = redisKey(callerId, idempotencyKey);
        String processing = serialize(IdempotencyRecord.processing());

        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, processing, processingTtl);
        if (Boolean.TRUE.equals(reserved)) {
            try {
                ApiResponse<OrderResponse> response = ApiResponse.of(action.get());
                redisTemplate.opsForValue().set(key, serialize(IdempotencyRecord.completed(response)), completedTtl);
                return response;
            } catch (RuntimeException ex) {
                redisTemplate.delete(key);
                throw ex;
            }
        }

        IdempotencyRecord current = readRecord(key);
        if (current != null && current.state() == IdempotencyState.COMPLETED && current.response() != null) {
            return current.response();
        }
        throw BusinessException.conflict(
                        "https://errors.ecom.local/idempotency-in-progress",
                        "Idempotency key is processing",
                        "A request with the same Idempotency-Key is still processing"
                )
                .withProperty("retryAfterSeconds", Math.max(1, processingTtl.toSeconds()));
    }

    private IdempotencyRecord readRecord(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, recordType);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize idempotency record for key " + key, ex);
        }
    }

    private String serialize(IdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize idempotency record", ex);
        }
    }

    private String redisKey(String callerId, String idempotencyKey) {
        return "idem:order:create:" + callerId + ":" + idempotencyKey;
    }
}
