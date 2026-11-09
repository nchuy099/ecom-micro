package com.nchuy099.ecom_micro_order_service.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nchuy099.ecom_micro_order_service.dto.BaseResponse;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderIdempotencyService {
    private final JdbcTemplate jdbc;
    private final OrderService orders;
    private final ObjectMapper mapper;

    public OrderIdempotencyService(JdbcTemplate jdbc, OrderService orders, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.mapper = mapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result create(String user, String key, CreateOrderReq request)
            throws JsonProcessingException {
        String hash = hash(request);
        Integer oldTimeout =
                jdbc.queryForObject("SELECT @@SESSION.innodb_lock_wait_timeout", Integer.class);
        try {
            jdbc.execute("SET SESSION innodb_lock_wait_timeout = 5");
            jdbc.update(
                    "INSERT INTO order_idempotency (user_id, idempotency_key, request_hash, status)"
                        + " VALUES (?, ?, ?, 'IN_PROGRESS') ON DUPLICATE KEY UPDATE user_id ="
                        + " user_id",
                    user,
                    key,
                    hash);
            Record record =
                    jdbc.queryForObject(
                            "SELECT request_hash, status, http_status, response_body, expires_at >"
                                + " CURRENT_TIMESTAMP(6) FROM order_idempotency WHERE user_id = ?"
                                + " AND idempotency_key = ? FOR UPDATE",
                            (rs, row) ->
                                    new Record(
                                            rs.getString(1),
                                            rs.getString(2),
                                            rs.getInt(3),
                                            rs.getString(4),
                                            rs.getBoolean(5)),
                            user,
                            key);
            if (record == null) throw new IllegalStateException("Idempotency record missing");
            if ("COMPLETED".equals(record.status()) && record.active()) {
                if (!record.hash().equals(hash)) throw new IdempotencyConflictException();
                return new Result(record.httpStatus(), record.body());
            }
            var order = orders.create(request);
            String body = mapper.writeValueAsString(new BaseResponse<>(order, "success"));
            jdbc.update(
                    "UPDATE order_idempotency SET request_hash = ?, status = 'COMPLETED',"
                        + " http_status = 200, response_body = ?, expires_at = CURRENT_TIMESTAMP(6)"
                        + " + INTERVAL 24 HOUR WHERE user_id = ? AND idempotency_key = ?",
                    hash,
                    body,
                    user,
                    key);
            return new Result(200, body);
        } finally {
            jdbc.execute("SET SESSION innodb_lock_wait_timeout = " + oldTimeout);
        }
    }

    private String hash(CreateOrderReq request) throws JsonProcessingException {
        ObjectNode canonical = mapper.createObjectNode();
        canonical.put("customerId", request.getCustomerId());
        var items = canonical.putArray("items");
        request.getItems()
                .forEach(
                        item -> {
                            ObjectNode entry = items.addObject();
                            entry.put("productId", item.getProductId());
                            entry.put("quantity", item.getQuantity());
                        });
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            mapper.writeValueAsString(canonical)
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Result(int status, String body) {}

    private record Record(
            String hash, String status, int httpStatus, String body, boolean active) {}

    public static class IdempotencyConflictException extends RuntimeException {}
}
