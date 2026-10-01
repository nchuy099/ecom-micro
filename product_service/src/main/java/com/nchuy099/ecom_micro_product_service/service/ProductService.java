package com.nchuy099.ecom_micro_product_service.service;

import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;

import java.util.List;

public interface ProductService {

    ProductEntity create(CreateProductReq req);

    void lock(LockProductReq lockProductReq);

    List<ProductEntity> search(ProductFilter productFilter);
}
