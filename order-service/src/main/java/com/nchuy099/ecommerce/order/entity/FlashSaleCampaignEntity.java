package com.nchuy099.ecommerce.order.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "flash_sale_campaigns")
public class FlashSaleCampaignEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "promo_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal promoPrice;

    @Column(name = "max_per_user", nullable = false)
    private Integer maxPerUser;

    @Column(name = "notification_subject", length = 255)
    private String notificationSubject;

    @Column(name = "notification_message", length = 1000)
    private String notificationMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FlashSaleCampaignEntity() {
    }

    public FlashSaleCampaignEntity(Long productId, Integer stock, Instant startsAt, Instant endsAt,
                                   BigDecimal promoPrice, Integer maxPerUser) {
        this(productId, stock, startsAt, endsAt, promoPrice, maxPerUser, null, null);
    }

    public FlashSaleCampaignEntity(Long productId, Integer stock, Instant startsAt, Instant endsAt,
                                   BigDecimal promoPrice, Integer maxPerUser,
                                   String notificationSubject, String notificationMessage) {
        this.productId = productId;
        this.stock = stock;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.promoPrice = promoPrice;
        this.maxPerUser = maxPerUser;
        this.notificationSubject = notificationSubject;
        this.notificationMessage = notificationMessage;
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

    public boolean isActiveAt(Instant instant) {
        return !instant.isBefore(startsAt) && instant.isBefore(endsAt);
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getStock() {
        return stock;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public BigDecimal getPromoPrice() {
        return promoPrice;
    }

    public Integer getMaxPerUser() {
        return maxPerUser;
    }

    public String getNotificationSubject() {
        return notificationSubject;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
