package com.nchuy099.ecommerce.order.controller;

import com.nchuy099.ecommerce.common.ApiResponse;
import com.nchuy099.ecommerce.order.dto.FlashSalePurchaseResponse;
import com.nchuy099.ecommerce.order.service.FlashSaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/flashsale")
public class FlashSaleController {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/{campaignId}/purchase")
    public ResponseEntity<ApiResponse<FlashSalePurchaseResponse>> purchase(
            @PathVariable Long campaignId,
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(flashSaleService.purchase(campaignId, userId)));
    }
}
