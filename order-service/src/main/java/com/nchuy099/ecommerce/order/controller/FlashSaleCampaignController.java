package com.nchuy099.ecommerce.order.controller;

import com.nchuy099.ecommerce.order.api.ApiResponse;
import com.nchuy099.ecommerce.order.dto.CreateFlashSaleCampaignRequest;
import com.nchuy099.ecommerce.order.dto.FlashSaleCampaignResponse;
import com.nchuy099.ecommerce.order.service.FlashSaleCampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/flashsale/campaigns")
public class FlashSaleCampaignController {
    private final FlashSaleCampaignService campaignService;

    public FlashSaleCampaignController(FlashSaleCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FlashSaleCampaignResponse> create(
            @Valid @RequestBody CreateFlashSaleCampaignRequest request
    ) {
        return ApiResponse.of(campaignService.create(request));
    }
}
