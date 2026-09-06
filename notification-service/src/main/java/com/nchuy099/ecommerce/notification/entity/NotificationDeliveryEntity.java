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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "notification_delivery",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_delivery_identity",
                columnNames = {"campaign_id", "user_id", "channel"}
        )
)
public class NotificationDeliveryEntity {
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationDeliveryStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationDeliveryEntity() {
    }

    public NotificationDeliveryEntity(String campaignId, Long userId, NotificationChannel channel, String subject, String message) {
        this.campaignId = campaignId;
        this.userId = userId;
        this.channel = channel;
        this.subject = subject;
        this.message = message;
        this.status = NotificationDeliveryStatus.PENDING;
        this.attempts = 0;
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

    public void markSent(int attempts) {
        this.status = NotificationDeliveryStatus.SENT;
        this.attempts = attempts;
        this.lastError = null;
    }

    public void markFailed(int attempts, String error) {
        this.status = NotificationDeliveryStatus.FAILED;
        this.attempts = attempts;
        this.lastError = error;
    }

    public boolean isSent() {
        return status == NotificationDeliveryStatus.SENT;
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

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }
}
