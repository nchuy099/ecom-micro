package com.nchuy099.ecommerce.order.repository;

import com.nchuy099.ecommerce.order.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
}
