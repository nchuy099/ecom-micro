package com.nchuy099.ecommerce.product.service;

import java.util.List;

import com.nchuy099.ecommerce.product.dto.CreateProductRequest;
import com.nchuy099.ecommerce.product.dto.OrderStockReservationRequest;
import com.nchuy099.ecommerce.product.dto.PageResponse;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.ProductSearchRequest;
import com.nchuy099.ecommerce.product.dto.UpdateProductRequest;
import com.nchuy099.ecommerce.product.entity.ProductEntity;
import com.nchuy099.ecommerce.product.entity.StockReservationEntity;
import com.nchuy099.ecommerce.product.exception.BusinessException;
import com.nchuy099.ecommerce.product.repository.ProductRepository;
import com.nchuy099.ecommerce.product.repository.StockReservationRepository;
import com.nchuy099.ecommerce.product.config.ProductSearchProperties;
import com.nchuy099.ecommerce.product.search.ProductSearchService;
import lombok.RequiredArgsConstructor;
import com.nchuy099.ecommerce.product.specification.ProductSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;
    private final StockReservationRepository stockReservationRepository;
    private final ProductSearchService productSearchService;

    private final ProductSearchProperties searchProperties;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        try {
            ProductEntity product = new ProductEntity(
                    request.name(),
                    request.sku(),
                    request.price(),
                    request.stock(),
                    request.categoryId()
            );
            return ProductResponse.from(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict(
                    "https://errors.ecom.local/duplicate-sku",
                    "Duplicate SKU",
                    "Product SKU already exists"
            );
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productCacheService.get(id)
                .orElseGet(() -> {
                    ProductResponse product = productRepository.findById(id)
                            .map(ProductResponse::from)
                            .orElseThrow(() -> productNotFound(id));
                    productCacheService.put(id, product);
                    return product;
                });
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        ProductEntity product = productRepository.findById(id).orElseThrow(() -> productNotFound(id));
        product.setName(request.name());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategoryId(request.categoryId());
        try {
            ProductResponse response = ProductResponse.from(productRepository.saveAndFlush(product));
            productCacheService.evict(id);
            return response;
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict(
                    "https://errors.ecom.local/duplicate-sku",
                    "Duplicate SKU",
                    "Product SKU already exists"
            );
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw productNotFound(id);
        }
        productRepository.deleteById(id);
        productCacheService.evict(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchRequest request) {
        if ("elasticsearch".equalsIgnoreCase(searchProperties.backend())) {
            return productSearchService.search(request);
        }
        validatePriceRange(request);
        Specification<ProductEntity> specification = Specification.allOf(
                ProductSpecifications.keywordContains(request.keyword()),
                ProductSpecifications.hasCategory(request.categoryId()),
                ProductSpecifications.priceGreaterThanOrEqual(request.minPrice()),
                ProductSpecifications.priceLessThanOrEqual(request.maxPrice())
        );
        PageRequest pageRequest = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<ProductEntity> page = productRepository.findAll(specification, pageRequest);
        return PageResponse.of(
                page.getContent().stream().map(ProductResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    public ProductSearchService.ReindexResult reindexSearch() {
        return productSearchService.reindex();
    }

    @Transactional
    public ProductResponse reserveForOrder(Long id, OrderStockReservationRequest request) {
        ProductEntity product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> productNotFound(id));

        var existingReservation = stockReservationRepository.findByOrderIdAndProductIdForUpdate(
                request.orderId(), id
        );
        if (existingReservation.isPresent()) {
            if (existingReservation.get().isReleased()) {
                throw stockConflict(id);
            }
            return ProductResponse.from(product);
        }

        if (product.getStock() < request.quantity()) {
            throw insufficientStock(id, request.quantity(), product.getStock());
        }

        product.reserve(request.quantity());
        ProductEntity saved = productRepository.saveAndFlush(product);
        stockReservationRepository.saveAndFlush(new StockReservationEntity(
                request.orderId(),
                request.userId(),
                saved.getId(),
                request.quantity()
        ));
        productCacheService.evict(saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    public void releaseOrder(Long orderId) {
        releaseReservations(orderId);
    }

    private void releaseReservations(Long orderId) {
        List<StockReservationEntity> reservations = stockReservationRepository.findByOrderIdAndReleasedFalse(orderId);
        for (StockReservationEntity reservation : reservations) {
            ProductEntity product = productRepository.findByIdForUpdate(reservation.getProductId())
                    .orElseThrow(() -> productNotFound(reservation.getProductId()));
            product.release(reservation.getQuantity());
            ProductEntity saved = productRepository.saveAndFlush(product);
            productCacheService.evict(saved.getId());
            reservation.markReleased();
            stockReservationRepository.save(reservation);
        }
    }

    private static void validatePriceRange(ProductSearchRequest request) {
        if (request.minPrice() != null && request.maxPrice() != null && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw BusinessException.badRequest(
                    "https://errors.ecom.local/invalid-price-range",
                    "Invalid price range",
                    "minPrice must be less than or equal to maxPrice"
            );
        }
    }

    private static BusinessException productNotFound(Long id) {
        return BusinessException.notFound(
                "https://errors.ecom.local/product-not-found",
                "Product not found",
                "Product not found: " + id
        );
    }

    private static BusinessException insufficientStock(Long id, int requested, int available) {
        return BusinessException.conflict(
                "https://errors.ecom.local/insufficient-stock",
                "Insufficient stock",
                "Insufficient stock for product " + id + ": requested " + requested + ", available " + available
        );
    }

    private static BusinessException stockConflict(Long id) {
        return BusinessException.conflict(
                "https://errors.ecom.local/stock-conflict",
                "Stock conflict",
                "Concurrent stock modification for product: " + id
        );
    }
}
