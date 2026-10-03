package com.nchuy099.ecom_micro_order_service.clients.impl;

import com.nchuy099.ecom_micro_order_service.clients.ProductClient;
import com.nchuy099.ecom_micro_order_service.dto.BaseResponse;
import com.nchuy099.ecom_micro_order_service.dto.ProductResp;
import com.nchuy099.ecom_micro_order_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_order_service.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {

    private final WebClient.Builder webClientBuilder;
    @Override
    public List<ProductResp> getProductsByIds(ProductFilter productFilter) {
//        WebClient.Builder builder = WebClient.builder();
        BaseResponse<List<ProductResp>> response = webClientBuilder.build()
                    .post()
                    .uri("http://product-service/v1/products/search")
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
