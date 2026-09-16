package com.nchuy099.ecommerce.product.repository;

import java.util.List;

import com.nchuy099.ecommerce.product.entity.StockReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservationEntity, Long> {
    List<StockReservationEntity> findByOrderIdAndReleasedFalse(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReservationEntity r where r.orderId = :orderId and r.productId = :productId")
    java.util.Optional<StockReservationEntity> findByOrderIdAndProductIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("productId") Long productId
    );
}
