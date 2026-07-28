package com.nchuy099.ecommerce.product.repository;

import com.nchuy099.ecommerce.product.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
}
