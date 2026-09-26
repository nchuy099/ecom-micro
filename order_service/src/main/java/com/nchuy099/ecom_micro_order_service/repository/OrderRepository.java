package com.nchuy099.ecom_micro_order_service.repository;

import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
}
