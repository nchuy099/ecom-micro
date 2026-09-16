package com.nchuy099.ecommerce.product.controller;

import com.nchuy099.ecommerce.product.api.ApiResponse;
import com.nchuy099.ecommerce.product.dto.CreateProductRequest;
import com.nchuy099.ecommerce.product.dto.PageResponse;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.ProductSearchRequest;
import com.nchuy099.ecommerce.product.dto.ReindexResponse;
import com.nchuy099.ecommerce.product.dto.OrderStockReservationRequest;
import com.nchuy099.ecommerce.product.dto.UpdateProductRequest;
import com.nchuy099.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.of(productService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> findById(@PathVariable Long id) {
        return ApiResponse.of(productService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.of(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @PostMapping("/search")
    public ApiResponse<PageResponse<ProductResponse>> search(@Valid @RequestBody ProductSearchRequest request) {
        return ApiResponse.of(productService.search(request));
    }

    @PostMapping("/search/reindex")
    public ApiResponse<ReindexResponse> reindexSearch() {
        var result = productService.reindexSearch();
        return ApiResponse.of(new ReindexResponse(result.indexed(), result.index()));
    }

    @PostMapping("/{id}/reserve-for-order")
    public ApiResponse<ProductResponse> reserveForOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderStockReservationRequest request
    ) {
        return ApiResponse.of(productService.reserveForOrder(id, request));
    }

    @PostMapping("/reservations/{orderId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseOrder(@PathVariable Long orderId) {
        productService.releaseOrder(orderId);
    }
}
