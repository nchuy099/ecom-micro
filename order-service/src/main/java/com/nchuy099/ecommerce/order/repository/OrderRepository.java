package com.nchuy099.ecommerce.order.repository;

import java.util.Optional;

import com.nchuy099.ecommerce.order.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    Page<OrderEntity> findByUserId(Long userId, Pageable pageable);
}
