package com.nchuy099.ecommerce.order.repository;

import com.nchuy099.ecommerce.order.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
