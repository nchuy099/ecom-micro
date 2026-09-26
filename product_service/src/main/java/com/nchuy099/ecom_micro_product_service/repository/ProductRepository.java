package com.nchuy099.ecom_micro_product_service.repository;

import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {
}
