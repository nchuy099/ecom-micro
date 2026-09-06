package com.nchuy099.ecommerce.notification.entity;

import java.time.Instant;

import com.nchuy099.ecommerce.common.event.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_dlq")
public class NotificationDlqEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false, length = 128)
    private String campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "failure_reason", nullable = false, length = 500)
    private String failureReason;

    @Column(nullable = false)
    private Integer attempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationDlqStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationDlqEntity() {
    }

    public NotificationDlqEntity(
            String campaignId,
            Long userId,
            NotificationChannel channel,
            String subject,
            String message,
            String payload,
            String failureReason,
            Integer attempts
    ) {
        this.campaignId = campaignId;
        this.userId = userId;
        this.channel = channel;
        this.subject = subject;
        this.message = message;
        this.payload = payload;
        this.failureReason = failureReason;
        this.attempts = attempts;
        this.status = NotificationDlqStatus.OPEN;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markReplayed() {
        status = NotificationDlqStatus.REPLAYED;
    }

    public Long getId() {
        return id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getPayload() {
        return payload;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public NotificationDlqStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
