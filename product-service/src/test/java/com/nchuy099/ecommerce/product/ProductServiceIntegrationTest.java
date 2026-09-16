package com.nchuy099.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.nchuy099.ecommerce.product.dto.CreateProductRequest;
import com.nchuy099.ecommerce.product.dto.OrderStockReservationRequest;
import com.nchuy099.ecommerce.product.dto.PageResponse;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.ProductSearchRequest;
import com.nchuy099.ecommerce.product.dto.UpdateProductRequest;
import com.nchuy099.ecommerce.product.entity.ProductEntity;
import com.nchuy099.ecommerce.product.exception.BusinessException;
import com.nchuy099.ecommerce.product.repository.ProductRepository;
import com.nchuy099.ecommerce.product.repository.StockReservationRepository;
import com.nchuy099.ecommerce.product.service.ProductService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "eureka.client.enabled=false",
        "ecommerce.search.backend=jpa"
})
class ProductServiceIntegrationTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        stockReservationRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void createsAndReadsProduct() {
        ProductResponse created = createProduct("Laptop", "SKU-1", "1200.00", 5, 10L);

        ProductResponse found = productService.findById(created.id());

        assertThat(found.name()).isEqualTo("Laptop");
        assertThat(found.sku()).isEqualTo("SKU-1");
        assertThat(found.price()).isEqualByComparingTo("1200.00");
        assertThat(found.stock()).isEqualTo(5);
        assertThat(found.version()).isNotNull();
        assertThat(found.createdAt()).isNotNull();
        assertThat(found.updatedAt()).isNotNull();
    }

    @Test
    void updatesAndDeletesProduct() {
        ProductResponse created = createProduct("Mouse", "SKU-2", "20.00", 10, 11L);

        ProductResponse updated = productService.update(created.id(), new UpdateProductRequest(
                "Mouse Pro",
                "SKU-2A",
                new BigDecimal("25.00"),
                12,
                12L
        ));
        productService.delete(created.id());

        assertThat(updated.name()).isEqualTo("Mouse Pro");
        assertThat(updated.sku()).isEqualTo("SKU-2A");
        assertThat(updated.stock()).isEqualTo(12);
        assertThatThrownBy(() -> productService.findById(created.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsDuplicateSku() {
        createProduct("Keyboard", "DUP-1", "30.00", 3, null);

        assertThatThrownBy(() -> createProduct("Keyboard 2", "DUP-1", "40.00", 4, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void searchesByKeywordCategoryAndPriceRange() {
        createProduct("Gaming Laptop", "LAP-GAME", "1500.00", 5, 100L);
        createProduct("Office Laptop", "LAP-OFFICE", "900.00", 8, 100L);
        createProduct("Phone", "PHONE-1", "700.00", 9, 200L);

        PageResponse<ProductResponse> result = productService.search(new ProductSearchRequest(
                "laptop",
                100L,
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                0,
                10
        ));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).extracting(ProductResponse::sku)
                .containsExactly("LAP-GAME");
    }

    @Test
    void rejectsInvalidPriceRange() {
        assertThatThrownBy(() -> productService.search(new ProductSearchRequest(
                null,
                null,
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                0,
                10
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void synchronouslyReservesStockWithProductLockAndIsIdempotent() {
        ProductResponse created = createProduct("Locked", "LOCK-1", "99.00", 5, null);
        OrderStockReservationRequest request = new OrderStockReservationRequest(7000L, 2000L, 2);

        ProductResponse first = productService.reserveForOrder(created.id(), request);
        ProductResponse retry = productService.reserveForOrder(created.id(), request);

        assertThat(first.stock()).isEqualTo(3);
        assertThat(retry.stock()).isEqualTo(3);
        assertThat(productService.findById(created.id()).stock()).isEqualTo(3);
        assertThat(stockReservationRepository.findByOrderIdAndReleasedFalse(7000L))
                .singleElement()
                .extracting(com.nchuy099.ecommerce.product.entity.StockReservationEntity::getQuantity)
                .isEqualTo(2);
    }

    @Test
    void optimisticLockingPreventsConcurrentOversell() {
        ProductResponse created = createProduct("Limited", "LIMITED-1", "99.00", 1, null);

        assertThatThrownBy(() -> simulateTwoConcurrentReserves(created.id()))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(productService.findById(created.id()).stock()).isEqualTo(0);
    }

    private void simulateTwoConcurrentReserves(Long productId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        ProductEntity first = tx.execute(status -> productRepository.findById(productId).orElseThrow());
        ProductEntity second = tx.execute(status -> productRepository.findById(productId).orElseThrow());

        tx.executeWithoutResult(status -> {
            ProductEntity managed = productRepository.findById(first.getId()).orElseThrow();
            managed.reserve(1);
            productRepository.saveAndFlush(managed);
        });

        tx.executeWithoutResult(status -> {
            ProductEntity stale = entityManager.merge(second);
            stale.reserve(1);
            entityManager.flush();
        });
    }

    private ProductResponse createProduct(String name, String sku, String price, int stock, Long categoryId) {
        return productService.create(new CreateProductRequest(
                name,
                sku,
                new BigDecimal(price),
                stock,
                categoryId
        ));
    }
}
