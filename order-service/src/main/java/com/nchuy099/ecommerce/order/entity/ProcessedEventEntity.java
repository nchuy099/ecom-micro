package com.nchuy099.ecommerce.order.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {
    @Id
    @Column(nullable = false, length = 200)
    private String id;

    @Column(nullable = false, length = 120)
    private String type;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {
    }

    public ProcessedEventEntity(String id, String type) {
        this.id = id;
        this.type = type;
    }

    @PrePersist
    void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }
}
