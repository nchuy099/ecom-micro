package com.nchuy099.ecommerce.product.repository;

import java.util.List;

import com.nchuy099.ecommerce.product.entity.StockReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservationEntity, Long> {
    List<StockReservationEntity> findByOrderIdAndReleasedFalse(Long orderId);
}
