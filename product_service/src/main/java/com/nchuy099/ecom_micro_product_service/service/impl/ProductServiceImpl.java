package com.nchuy099.ecom_micro_product_service.service.impl;

import com.nchuy099.ecom_micro_product_service.dto.request.CreateProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductItem;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductReq;
import com.nchuy099.ecom_micro_product_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_product_service.dto.request.UpdateProductReq;
import com.nchuy099.ecom_micro_product_service.entity.ProductEntity;
import com.nchuy099.ecom_micro_product_service.exception.ApplicationException;
import com.nchuy099.ecom_micro_product_service.mapper.ProductMapper;
import com.nchuy099.ecom_micro_product_service.repository.CategoryRepository;
import com.nchuy099.ecom_micro_product_service.repository.ProductRepository;
import com.nchuy099.ecom_micro_product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final CategoryRepository categoryRepository;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;

    @Override
    public ProductEntity create(CreateProductReq req) {

        var existedCatOpt = categoryRepository.findById(req.getCategoryId());
        if (existedCatOpt.isEmpty()) {
            throw new ApplicationException("Category not found");
        }

        ProductEntity creatingProduct = productMapper.fromCreateProduct(req);
        return productRepository.save(creatingProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductEntity update(String id, UpdateProductReq req) {
        var product =
                productRepository
                        .findById(id)
                        .orElseThrow(() -> new ApplicationException("Product not found"));

        if (req.getName() != null) {
            product.setName(req.getName());
        }
        if (req.getPrice() != null) {
            product.setPrice(req.getPrice());
        }
        if (req.getStock() != null) {
            product.setStock(req.getStock());
        }
        if (req.getCategoryId() != null) {
            categoryRepository
                    .findById(req.getCategoryId())
                    .orElseThrow(() -> new ApplicationException("Category not found"));
            product.setCategoryId(req.getCategoryId());
        }

        return productRepository.save(product);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductEntity getById(String id) {
        log.info("Getting product from database: {}", id);
        return productRepository
                .findById(id)
                .orElseThrow(() -> new ApplicationException("Product not found"));
    }

    @Override
    @Transactional
    public void lock(LockProductReq lockProductReq) {
        List<LockProductItem> items = lockProductReq.getItems();
        log.info("product.stock.lock.start productCount={}", items.size());

        // Sắp xếp ID để tránh deadlock giữa các request
        List<String> sortedIds =
                items.stream().map(LockProductItem::getId).sorted().collect(Collectors.toList());

        String lockKey = "lock:products:" + String.join(",", sortedIds);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Thử lấy lock trong 10s, giữ lock trong 5s
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                Thread.sleep(4000);
                log.info("product.redis.lock.acquired lockKey={}", lockKey);

                Map<String, Integer> productIdQuantityMap =
                        items.stream()
                                .collect(
                                        Collectors.toMap(
                                                LockProductItem::getId,
                                                item -> item.getQuantity(),
                                                Integer::sum));

                // Lúc này không dùng findByIdInForUpdate nữa, chỉ dùng findByIdIn thường
                List<ProductEntity> products =
                        productRepository.findByIdIn(
                                new ArrayList<>(productIdQuantityMap.keySet()));

                if (products.isEmpty()) {
                    throw new RuntimeException("Product not found");
                }

                // Khấu trừ tồn kho trong memory
                products.forEach(
                        product -> {
                            int remainStock =
                                    product.getStock() - productIdQuantityMap.get(product.getId());
                            if (remainStock < 0) {
                                throw new RuntimeException(
                                        "Product " + product.getId() + " is out of stock");
                            }
                            product.setStock(remainStock);
                        });

                // Lưu xuống DB
                productRepository.saveAll(products);
                log.info(
                        "product.stock.updated productCount={} lockKey={}",
                        products.size(),
                        lockKey);

                // Chỉ invalidate cache của các product vừa thay đổi tồn kho
                Cache productCache = cacheManager.getCache("products");
                if (productCache != null) {
                    productIdQuantityMap.keySet().forEach(productCache::evict);
                }
            } else {
                log.warn("product.stock.lock.failed reason=server_busy lockKey={}", lockKey);
                throw new RuntimeException("Server busy, please try again later");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error(
                    "product.stock.lock.failed reason=interrupted lockKey={}", lockKey, exception);
            throw new RuntimeException("Process interrupted");
        } finally {
            // Giải phóng lock
            log.info("product.redis.lock.releasing lockKey={}", lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("product.redis.lock.released lockKey={}", lockKey);
            }
        }
    }

    @Override
    public List<ProductEntity> search(ProductFilter productFilter) {
        return productRepository.findByIdIn(productFilter.getIds());
    }
}
