package com.nchuy099.ecommerce.order.repository;

import java.util.List;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignRecipientEntity;
import com.nchuy099.ecommerce.order.entity.FlashSaleRecipientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashSaleCampaignRecipientRepository extends JpaRepository<FlashSaleCampaignRecipientEntity, Long> {
    List<FlashSaleCampaignRecipientEntity> findByCampaignIdAndStatusOrderByIdAsc(
            Long campaignId, FlashSaleRecipientStatus status, Pageable pageable);

    long countByCampaignId(Long campaignId);
}
