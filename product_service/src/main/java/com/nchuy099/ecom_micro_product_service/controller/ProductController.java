package com.nchuy099.ecom_micro_product_service.controller;

import com.nchuy099.ecom_micro_product_service.dto.BaseResponse;
import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_product_service.dto.request.UpdateProductReq;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import com.nchuy099.ecom_micro_product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<BaseResponse<ProductEntity>> create(@RequestBody @Valid CreateProductReq req) {
        return ResponseEntity.ok(new BaseResponse<>(productService.create(req), "success" ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductEntity>> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateProductReq req) {
        return ResponseEntity.ok(new BaseResponse<>(productService.update(id, req), "success"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductEntity>> getById(@PathVariable String id) {
        return ResponseEntity.ok(new BaseResponse<>(productService.getById(id), "success"));
    }

    @PostMapping("/lock")
    public ResponseEntity<BaseResponse<Boolean>> lock(@RequestBody @Valid LockProductReq lockProductReq) {
        productService.lock(lockProductReq);
        return ResponseEntity.ok(new BaseResponse<>(true, "success"));
    }

    @PostMapping("/search")
    public ResponseEntity<BaseResponse<List<ProductEntity>>> search(@RequestBody ProductFilter productFilter) {
        List<ProductEntity> products = productService.search(productFilter);
        return ResponseEntity.ok(new BaseResponse<>(products, "success"));
    }
}
