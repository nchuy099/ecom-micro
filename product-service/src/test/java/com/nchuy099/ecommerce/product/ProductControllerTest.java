package com.nchuy099.ecommerce.product;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.product.controller.ProductController;
import com.nchuy099.ecommerce.product.dto.CreateProductRequest;
import com.nchuy099.ecommerce.product.dto.PageResponse;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.ProductSearchRequest;
import com.nchuy099.ecommerce.product.dto.StockQuantityRequest;
import com.nchuy099.ecommerce.product.dto.UpdateProductRequest;
import com.nchuy099.ecommerce.product.exception.DuplicateSkuException;
import com.nchuy099.ecommerce.product.exception.InsufficientStockException;
import com.nchuy099.ecommerce.product.exception.ProductNotFoundException;
import com.nchuy099.ecommerce.product.exception.StockConflictException;
import com.nchuy099.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void createsProduct() throws Exception {
        when(productService.create(any())).thenReturn(productResponse(1L, "Laptop", "SKU-1", 5));

        mockMvc.perform(post("/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Laptop",
                                "SKU-1",
                                new BigDecimal("1200.00"),
                                5,
                                10L
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sku").value("SKU-1"));
    }

    @Test
    void readsUpdatesDeletesSearchesReservesAndReleases() throws Exception {
        when(productService.findById(1L)).thenReturn(productResponse(1L, "Laptop", "SKU-1", 5));
        when(productService.update(eq(1L), any())).thenReturn(productResponse(1L, "Laptop Pro", "SKU-1A", 8));
        when(productService.search(any())).thenReturn(PageResponse.of(List.of(productResponse(1L, "Laptop", "SKU-1", 5)), 0, 20, 1));
        when(productService.reserve(eq(1L), any())).thenReturn(productResponse(1L, "Laptop", "SKU-1", 3));
        when(productService.release(eq(1L), any())).thenReturn(productResponse(1L, "Laptop", "SKU-1", 4));
        doNothing().when(productService).delete(1L);

        mockMvc.perform(get("/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop"));

        mockMvc.perform(put("/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProductRequest(
                                "Laptop Pro",
                                "SKU-1A",
                                new BigDecimal("1300.00"),
                                8,
                                10L
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-1A"));

        mockMvc.perform(post("/v1/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductSearchRequest("lap", 10L, null, null, 0, 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(post("/v1/products/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockQuantityRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(3));

        mockMvc.perform(post("/v1/products/1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockQuantityRequest(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(4));

        mockMvc.perform(delete("/v1/products/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void returnsProblemDetailForValidationFailure() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "sku": "",
                                  "price": -1,
                                  "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returnsProblemDetailForDomainErrors() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));
        when(productService.create(any())).thenThrow(new DuplicateSkuException());
        when(productService.reserve(eq(1L), any())).thenThrow(new InsufficientStockException(1L, 2, 1));
        when(productService.release(eq(1L), any())).thenThrow(new StockConflictException(1L));

        mockMvc.perform(get("/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"));

        mockMvc.perform(post("/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Laptop",
                                "SKU-1",
                                new BigDecimal("1200.00"),
                                5,
                                null
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate SKU"));

        mockMvc.perform(post("/v1/products/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockQuantityRequest(2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"));

        mockMvc.perform(post("/v1/products/1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockQuantityRequest(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Stock conflict"));
    }

    private static ProductResponse productResponse(Long id, String name, String sku, int stock) {
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        return new ProductResponse(
                id,
                name,
                sku,
                new BigDecimal("1200.00"),
                stock,
                10L,
                0L,
                now,
                now
        );
    }
}
