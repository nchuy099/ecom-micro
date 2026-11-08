package com.nchuy099.ecom_micro_order_service.repository;

import com.nchuy099.ecom_micro_order_service.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {
    java.util.List<OrderItemEntity> findByOrderId(String orderId);
}
