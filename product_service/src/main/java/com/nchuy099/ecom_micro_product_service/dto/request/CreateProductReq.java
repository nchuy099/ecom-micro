package com.nchuy099.ecom_micro_product_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductReq {
    @NotEmpty
    private String name;

    @NotNull
    @Positive
    private Integer price;

    @NotNull
    @Positive
    private Integer stock;

    @NotEmpty
    private String categoryId;
}
