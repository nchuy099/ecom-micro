package com.nchuy099.ecommerce.product.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "aggregate_type", nullable = false, length = 120)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(nullable = false, length = 120)
    private String type;

    @Column(nullable = false, length = 4096)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(UUID id, String aggregateType, String aggregateId, String type, String payload, Instant occurredAt) {
        this.id = id.toString();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public String getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }
}
