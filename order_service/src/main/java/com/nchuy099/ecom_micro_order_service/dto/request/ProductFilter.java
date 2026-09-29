package com.nchuy099.ecom_micro_order_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductFilter {
    @NotEmpty
    private List<String> ids;
}
