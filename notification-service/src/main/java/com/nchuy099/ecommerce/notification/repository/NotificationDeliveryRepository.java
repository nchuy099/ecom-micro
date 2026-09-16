package com.nchuy099.ecommerce.notification.repository;

import java.util.Optional;

import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
    Optional<NotificationDeliveryEntity> findByCampaignIdAndUserIdAndChannel(
            String campaignId,
            Long userId,
            NotificationChannel channel
    );

    boolean existsByCampaignIdAndUserIdAndChannel(String campaignId, Long userId, NotificationChannel channel);
}
