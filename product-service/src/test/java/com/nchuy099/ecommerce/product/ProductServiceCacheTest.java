package com.nchuy099.ecommerce.product;

import java.math.BigDecimal;
import java.util.Optional;

import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.UpdateProductRequest;
import com.nchuy099.ecommerce.product.entity.ProductEntity;
import com.nchuy099.ecommerce.product.repository.ProductRepository;
import com.nchuy099.ecommerce.product.repository.StockReservationRepository;
import com.nchuy099.ecommerce.product.service.ProductCacheService;
import com.nchuy099.ecommerce.product.service.ProductService;
import com.nchuy099.ecommerce.product.search.ProductSearchService;
import com.nchuy099.ecommerce.product.config.ProductSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceCacheTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private ProductSearchService productSearchService;

    @Mock
    private ProductSearchProperties searchProperties;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                productCacheService,
                stockReservationRepository,
                productSearchService,
                searchProperties
        );
    }

    @Test
    void findByIdReturnsCachedValueWithoutQueryingRepository() {
        ProductResponse cachedProduct = new ProductResponse(
                1L, "Headphones", "HP-1", new BigDecimal("50.00"), 5, null, 0L, null, null
        );
        when(productCacheService.get(1L)).thenReturn(Optional.of(cachedProduct));

        ProductResponse result = productService.findById(1L);

        assertThat(result).isEqualTo(cachedProduct);
        verify(productRepository, never()).findById(any());
        verify(productCacheService, never()).put(any(), any());
    }

    @Test
    void findByIdQueriesRepositoryAndPopulatesCacheOnCacheMiss() {
        ProductEntity entity = new ProductEntity("Headphones", "HP-1", new BigDecimal("50.00"), 5, null);
        when(productCacheService.get(1L)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));

        ProductResponse result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("Headphones");
        verify(productRepository, times(1)).findById(1L);
        verify(productCacheService, times(1)).put(eq(1L), any(ProductResponse.class));
    }

    @Test
    void updateInvalidatesCacheAfterSuccessfulSave() {
        ProductEntity entity = new ProductEntity("Headphones", "HP-1", new BigDecimal("50.00"), 5, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(productRepository.saveAndFlush(any(ProductEntity.class))).thenReturn(entity);

        UpdateProductRequest request = new UpdateProductRequest(
                "Headphones V2", "HP-2", new BigDecimal("60.00"), 8, null
        );

        productService.update(1L, request);

        verify(productRepository).saveAndFlush(entity);
        verify(productCacheService).evict(1L);
    }

    @Test
    void deleteInvalidatesCache() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
        verify(productCacheService).evict(1L);
    }
}
