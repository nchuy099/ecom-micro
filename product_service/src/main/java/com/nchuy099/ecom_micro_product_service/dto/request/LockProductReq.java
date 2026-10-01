package com.nchuy099.ecom_micro_product_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LockProductReq {

    @NotEmpty
    private List<@Valid LockProductItem> items;
}
