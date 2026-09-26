package com.nchuy099.ecom_micro_product_service.service;

import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;

public interface ProductService {

    ProductEntity create(CreateProductReq req);
}
