package com.nchuy099.ecom_micro_order_service.clients.impl;

import com.nchuy099.ecom_micro_order_service.clients.ProductClient;
import com.nchuy099.ecom_micro_order_service.dto.BaseResponse;
import com.nchuy099.ecom_micro_order_service.dto.ProductResp;
import com.nchuy099.ecom_micro_order_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_order_service.exception.ApplicationException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class ProductClientImpl implements ProductClient {

    @Override
    public List<ProductResp> getProductsByIds(ProductFilter productFilter) {
        WebClient.Builder builder = WebClient.builder();
        BaseResponse<List<ProductResp>> response = builder.build()
                    .post()
                    .uri("http://localhost:8888/v1/products/search")
                    .bodyValue(productFilter)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<ProductResp>>>() {
                    })
                    .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException("Không thể lấy thông tin sản phẩm từ Product Service");
        }

        return response.getData();
    }
}
