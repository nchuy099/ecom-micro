package com.nchuy099.ecom_micro_product_service.service.impl;

import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import com.nchuy099.ecom_micro_product_service.exception.ApplicationException;
import com.nchuy099.ecom_micro_product_service.mapper.ProductMapper;
import com.nchuy099.ecom_micro_product_service.repository.CategoryRepository;
import com.nchuy099.ecom_micro_product_service.repository.ProductRepository;
import com.nchuy099.ecom_micro_product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public void lock(LockProductReq lockProductReq) {
        Map<String, Integer> productIdQuantityMap = lockProductReq.getItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId(),
                        item -> item.getQuantity(),
                        Integer::sum));

        List<ProductEntity> products = productRepository
                .findByIdInForUpdate(List.copyOf(productIdQuantityMap.keySet()));

        if (products.size() != productIdQuantityMap.size()) {
            throw new ApplicationException("Product not found");
        }

        products.forEach(product -> {
            int quantity = productIdQuantityMap.get(product.getId());
            if (product.getStock() < quantity) {
                throw new ApplicationException("Product " + product.getId() + " not enough");
            }
            product.setStock(product.getStock() - quantity);
        });

        productRepository.saveAll(products);
    }

    @Override
    public List<ProductEntity> search(ProductFilter productFilter) {
        return productRepository.findByIdIn(productFilter.getIds());
    }
}
