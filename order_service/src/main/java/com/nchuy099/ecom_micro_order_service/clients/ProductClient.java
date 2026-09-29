package com.nchuy099.ecom_micro_order_service.clients;

import com.nchuy099.ecom_micro_order_service.dto.ProductResp;
import com.nchuy099.ecom_micro_order_service.dto.request.ProductFilter;

import java.util.List;

public interface ProductClient {

    List<ProductResp> getProductsByIds(ProductFilter productFilter);
}
