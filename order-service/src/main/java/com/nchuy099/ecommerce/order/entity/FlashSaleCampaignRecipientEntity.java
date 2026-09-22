package com.nchuy099.ecommerce.order.entity;

import java.time.Instant;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "flash_sale_campaign_recipients", uniqueConstraints = @UniqueConstraint(
        name = "uk_flash_sale_campaign_recipient",
        columnNames = {"campaign_id", "user_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashSaleCampaignRecipientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FlashSaleRecipientStatus status;

    @Column(name = "task_published_at")
    private Instant taskPublishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FlashSaleCampaignRecipientEntity(Long campaignId, Long userId) {
        this.campaignId = campaignId;
        this.userId = userId;
        this.status = FlashSaleRecipientStatus.PENDING;
    }

    public void markPublished(Instant publishedAt) {
        this.status = FlashSaleRecipientStatus.PUBLISHED;
        this.taskPublishedAt = publishedAt;
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

}
