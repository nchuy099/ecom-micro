package com.nchuy099.ecommerce.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.nchuy099.ecommerce.common.event.OrderCancelledEvent;
import com.nchuy099.ecommerce.common.event.OrderCreatedEvent;
import com.nchuy099.ecommerce.common.event.OrderItemRequested;
import com.nchuy099.ecommerce.common.event.StockReleasedEvent;
import com.nchuy099.ecommerce.common.event.StockReservationFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedItem;
import com.nchuy099.ecommerce.product.dto.CreateProductRequest;
import com.nchuy099.ecommerce.product.dto.PageResponse;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.ProductSearchRequest;
import com.nchuy099.ecommerce.product.dto.StockQuantityRequest;
import com.nchuy099.ecommerce.product.dto.UpdateProductRequest;
import com.nchuy099.ecommerce.product.entity.ProductEntity;
import com.nchuy099.ecommerce.product.entity.StockReservationEntity;
import com.nchuy099.ecommerce.product.exception.DuplicateSkuException;
import com.nchuy099.ecommerce.product.exception.InsufficientStockException;
import com.nchuy099.ecommerce.product.exception.InvalidPriceRangeException;
import com.nchuy099.ecommerce.product.exception.ProductNotFoundException;
import com.nchuy099.ecommerce.product.exception.StockConflictException;
import com.nchuy099.ecommerce.product.repository.ProductRepository;
import com.nchuy099.ecommerce.product.repository.StockReservationRepository;
import com.nchuy099.ecommerce.product.specification.ProductSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;
    private final StockReservationRepository stockReservationRepository;
    private final OutboxEventService outboxEventService;
    private final ProcessedEventService processedEventService;

    public ProductService(
            ProductRepository productRepository,
            ProductCacheService productCacheService,
            StockReservationRepository stockReservationRepository,
            OutboxEventService outboxEventService,
            ProcessedEventService processedEventService
    ) {
        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
        this.stockReservationRepository = stockReservationRepository;
        this.outboxEventService = outboxEventService;
        this.processedEventService = processedEventService;
    }

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
            throw new DuplicateSkuException();
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productCacheService.get(id)
                .orElseGet(() -> {
                    ProductResponse product = productRepository.findById(id)
                            .map(ProductResponse::from)
                            .orElseThrow(() -> new ProductNotFoundException(id));
                    productCacheService.put(id, product);
                    return product;
                });
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        ProductEntity product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
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
            throw new DuplicateSkuException();
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        productCacheService.evict(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchRequest request) {
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

    @Transactional
    public ProductResponse reserve(Long id, StockQuantityRequest request) {
        ProductEntity product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (product.getStock() < request.quantity()) {
            throw new InsufficientStockException(id, request.quantity(), product.getStock());
        }
        product.reserve(request.quantity());
        try {
            ProductResponse response = ProductResponse.from(productRepository.saveAndFlush(product));
            productCacheService.evict(id);
            return response;
        } catch (OptimisticLockingFailureException ex) {
            throw new StockConflictException(id);
        }
    }

    @Transactional
    public ProductResponse release(Long id, StockQuantityRequest request) {
        ProductEntity product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.release(request.quantity());
        try {
            ProductResponse response = ProductResponse.from(productRepository.saveAndFlush(product));
            productCacheService.evict(id);
            return response;
        } catch (OptimisticLockingFailureException ex) {
            throw new StockConflictException(id);
        }
    }

    @Transactional
    public boolean reserveForOrder(OrderCreatedEvent event) {
        return processedEventService.processOnce(event.eventId(), event.eventType(), () -> {
            List<ProductEntity> products = new ArrayList<>();
            List<StockReservedItem> reservedItems = new ArrayList<>();

            for (OrderItemRequested item : event.items()) {
                ProductEntity product = productRepository.findById(item.productId()).orElse(null);
                if (product == null) {
                    outboxEventService.writeStockReservationFailed(StockReservationFailedEvent.of(
                            event.orderId(),
                            event.userId(),
                            "Product not found: " + item.productId()
                    ));
                    return true;
                }
                if (product.getStock() < item.quantity()) {
                    outboxEventService.writeStockReservationFailed(StockReservationFailedEvent.of(
                            event.orderId(),
                            event.userId(),
                            "Insufficient stock for product " + item.productId()
                    ));
                    return true;
                }
                products.add(product);
            }

            for (int i = 0; i < event.items().size(); i++) {
                OrderItemRequested item = event.items().get(i);
                ProductEntity product = products.get(i);
                product.reserve(item.quantity());
                ProductEntity saved = productRepository.saveAndFlush(product);
                productCacheService.evict(saved.getId());
                stockReservationRepository.save(new StockReservationEntity(
                        event.orderId(),
                        event.userId(),
                        saved.getId(),
                        item.quantity()
                ));
                BigDecimal subtotal = saved.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
                reservedItems.add(new StockReservedItem(
                        saved.getId(),
                        saved.getName(),
                        item.quantity(),
                        saved.getPrice(),
                        subtotal
                ));
            }

            outboxEventService.writeStockReserved(StockReservedEvent.of(event.orderId(), event.userId(), reservedItems));
            return true;
        });
    }

    @Transactional
    public boolean releaseForOrder(OrderCancelledEvent event) {
        return processedEventService.processOnce(event.eventId(), event.eventType(), () -> {
            List<StockReservationEntity> reservations = stockReservationRepository.findByOrderIdAndReleasedFalse(event.orderId());
            for (StockReservationEntity reservation : reservations) {
                ProductEntity product = productRepository.findById(reservation.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(reservation.getProductId()));
                product.release(reservation.getQuantity());
                ProductEntity saved = productRepository.saveAndFlush(product);
                productCacheService.evict(saved.getId());
                reservation.markReleased();
                stockReservationRepository.save(reservation);
            }
            if (!reservations.isEmpty()) {
                outboxEventService.writeStockReleased(StockReleasedEvent.of(event.orderId(), event.userId()));
            }
            return true;
        });
    }

    private static void validatePriceRange(ProductSearchRequest request) {
        if (request.minPrice() != null && request.maxPrice() != null && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new InvalidPriceRangeException();
        }
    }
}
