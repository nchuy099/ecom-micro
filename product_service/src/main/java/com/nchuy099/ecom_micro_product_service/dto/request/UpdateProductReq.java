package com.nchuy099.ecom_micro_product_service.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductReq {

    private String name;

    @Positive
    private Integer price;

    @Positive
    private Integer stock;

    private String categoryId;
}
