package com.nchuy099.ecommerce.order;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nchuy099.ecommerce.order.controller.FlashSaleController;
import com.nchuy099.ecommerce.order.dto.FlashSalePurchaseResponse;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.service.FlashSaleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FlashSaleController.class)
class FlashSaleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlashSaleService flashSaleService;

    @Test
    void purchaseReturnsAccepted() throws Exception {
        when(flashSaleService.purchase(1L, 100L))
                .thenReturn(new FlashSalePurchaseResponse(1L, 10L, 20L, "ACCEPTED"));

        mockMvc.perform(post("/v1/flashsale/1/purchase")
                        .header("X-User-Id", "100"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.campaignId").value(1))
                .andExpect(jsonPath("$.data.productId").value(10))
                .andExpect(jsonPath("$.data.orderId").value(20))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void soldOutReturnsConflictProblemDetail() throws Exception {
        when(flashSaleService.purchase(1L, 100L))
                .thenThrow(BusinessException.conflict(
                                "https://errors.ecom.local/flash-sale-sold-out",
                                "SOLD_OUT",
                                "Flash sale campaign is sold out")
                        .withProperty("reason", "SOLD_OUT"));

        mockMvc.perform(post("/v1/flashsale/1/purchase")
                        .header("X-User-Id", "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("SOLD_OUT"))
                .andExpect(jsonPath("$.reason").value("SOLD_OUT"));
    }

    @Test
    void alreadyPurchasedReturnsConflictProblemDetail() throws Exception {
        when(flashSaleService.purchase(1L, 100L))
                .thenThrow(BusinessException.conflict(
                                "https://errors.ecom.local/flash-sale-already-purchased",
                                "ALREADY_PURCHASED",
                                "Flash sale purchase limit already reached")
                        .withProperty("reason", "ALREADY_PURCHASED"));

        mockMvc.perform(post("/v1/flashsale/1/purchase")
                        .header("X-User-Id", "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("ALREADY_PURCHASED"))
                .andExpect(jsonPath("$.reason").value("ALREADY_PURCHASED"));
    }
}
