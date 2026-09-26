package com.nchuy099.ecom_micro_product_service.repository;

import com.nchuy099.ecom_micro_product_service.entity.CategoryEntity;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {
}
