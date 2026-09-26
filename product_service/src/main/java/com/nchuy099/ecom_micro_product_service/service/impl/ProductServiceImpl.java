package com.nchuy099.ecom_micro_product_service.service.impl;

import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import com.nchuy099.ecom_micro_product_service.exception.ApplicationException;
import com.nchuy099.ecom_micro_product_service.mapper.ProductMapper;
import com.nchuy099.ecom_micro_product_service.repository.CategoryRepository;
import com.nchuy099.ecom_micro_product_service.repository.ProductRepository;
import com.nchuy099.ecom_micro_product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final CategoryRepository categoryRepository;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductEntity create(CreateProductReq req) {

        var existedCatOpt = categoryRepository.findById(req.getCategoryId());
        if (existedCatOpt.isEmpty()) {
            throw new ApplicationException("Category not found");
        }

        ProductEntity creatingProduct = productMapper.fromCreateProduct(req);
        return  productRepository.save(creatingProduct);

    }
}
