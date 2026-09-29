package com.nchuy099.ecom_micro_product_service.repository;

import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    List<ProductEntity> findByIdIn(List<String> ids);
}
