package com.nchuy099.ecommerce.order.repository;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface FlashSaleCampaignRepository extends JpaRepository<FlashSaleCampaignEntity, Long> {
    List<FlashSaleCampaignEntity> findByStartsAtBetween(Instant from, Instant to);
}
