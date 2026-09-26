package com.nchuy099.ecom_micro_product_service.mapper;

import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductEntity fromCreateProduct(CreateProductReq req);
}
