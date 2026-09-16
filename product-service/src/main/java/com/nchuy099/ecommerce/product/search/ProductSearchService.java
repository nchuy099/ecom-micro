package com.nchuy099.ecommerce.product.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.nchuy099.ecommerce.product.dto.PageResponse;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.dto.ProductSearchRequest;
import com.nchuy099.ecommerce.product.entity.ProductEntity;
import com.nchuy099.ecommerce.product.exception.BusinessException;
import com.nchuy099.ecommerce.product.repository.ProductRepository;
import com.nchuy099.ecommerce.product.config.ProductSearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ElasticsearchClient client;
    private final ProductRepository productRepository;
    private final ProductSearchProperties properties;

    public PageResponse<ProductResponse> search(ProductSearchRequest request) {
        if (request.cursor() != null && request.page() != null) {
            throw BusinessException.badRequest(
                    "https://errors.ecom.local/invalid-pagination",
                    "Invalid pagination request",
                    "page and cursor cannot be used together"
            );
        }
        Long cursorId = request.cursor() == null ? null : ProductCursorCodec.decode(request.cursor());
        try {
            int size = request.sizeOrDefault();
            SearchResponse<ProductDocument> response = client.search(builder -> {
                builder.index(properties.indexAlias())
                        .size(size + 1)
                        .trackTotalHits(track -> track.enabled(request.cursor() == null))
                        .sort(sort -> sort.field(field -> field.field("id").order(SortOrder.Desc)))
                        .query(query(request));
                if (cursorId != null) {
                    builder.searchAfter(List.of(FieldValue.of(cursorId)));
                } else if (request.page() != null && request.page() > 0) {
                    builder.from(request.page() * size);
                }
                return builder;
            }, ProductDocument.class);

            List<Hit<ProductDocument>> hits = response.hits().hits();
            boolean hasNext = hits.size() > size;
            List<ProductResponse> content = hits.stream()
                    .limit(size)
                    .map(Hit::source)
                    .filter(java.util.Objects::nonNull)
                    .map(ProductDocument::toResponse)
                    .toList();
            String nextCursor = hasNext && !content.isEmpty()
                    ? ProductCursorCodec.encode(content.get(content.size() - 1).id())
                    : null;

            if (request.cursor() != null) {
                return PageResponse.cursor(content, size, nextCursor, hasNext);
            }
            long total = response.hits().total() == null ? content.size() : response.hits().total().value();
            int page = request.pageOrDefault();
            return PageResponse.of(content, page, size, total);
        } catch (Exception ex) {
            throw new ProductSearchException("Product search index is unavailable or has not been reindexed", ex);
        }
    }

    public ReindexResult reindex() {
        String targetIndex = properties.indexAlias() + "-" + UUID.randomUUID();
        try {
            client.indices().create(builder -> builder.index(targetIndex));
            long lastId = 0L;
            long indexed = 0L;
            while (true) {
                List<ProductEntity> products = productRepository.findByIdGreaterThan(
                        lastId,
                        PageRequest.of(0, properties.reindexBatchSize(), Sort.by(Sort.Direction.ASC, "id"))
                );
                if (products.isEmpty()) {
                    break;
                }
                List<BulkOperation> operations = products.stream()
                        .map(product -> BulkOperation.of(operation -> operation.index(index -> index
                                .index(targetIndex)
                                .id(String.valueOf(product.getId()))
                                .document(ProductDocument.from(product)))))
                        .toList();
                var bulkResponse = client.bulk(builder -> builder.operations(operations));
                if (bulkResponse.errors()) {
                    throw new ProductSearchException("Elasticsearch bulk indexing returned partial failures");
                }
                indexed += products.size();
                lastId = products.get(products.size() - 1).getId();
            }

            Map<String, ?> aliases = existingIndexes();
            client.indices().updateAliases(builder -> {
                aliases.keySet().forEach(oldIndex -> builder.actions(action -> action.remove(remove -> remove
                        .index(oldIndex)
                        .alias(properties.indexAlias()))));
                builder.actions(action -> action.add(add -> add.index(targetIndex).alias(properties.indexAlias())));
                return builder;
            });
            aliases.keySet().forEach(oldIndex -> {
                try {
                    client.indices().delete(builder -> builder.index(oldIndex));
                } catch (Exception ignored) {
                    // Alias already points to the fresh index; cleanup can be retried safely.
                }
            });
            return new ReindexResult(indexed, targetIndex);
        } catch (Exception ex) {
            try {
                client.indices().delete(builder -> builder.index(targetIndex));
            } catch (Exception ignored) {
                // Preserve the original reindex failure.
            }
            throw new ProductSearchException("Product reindex failed", ex);
        }
    }

    private Map<String, ?> existingIndexes() {
        try {
            return client.indices().getAlias(builder -> builder.name(properties.indexAlias())).result();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Query query(ProductSearchRequest request) {
        List<Query> filters = new ArrayList<>();
        if (request.categoryId() != null) {
            filters.add(Query.of(query -> query.term(term -> term.field("categoryId").value(request.categoryId()))));
        }
        if (request.minPrice() != null || request.maxPrice() != null) {
            filters.add(Query.of(query -> query.range(range -> range.number(number -> number
                    .field("price")
                    .gte(request.minPrice() == null ? null : request.minPrice().doubleValue())
                    .lte(request.maxPrice() == null ? null : request.maxPrice().doubleValue())))));
        }
        Query base = Query.of(query -> query.bool(bool -> bool.filter(filters)));
        if (request.keyword() == null || request.keyword().isBlank()) {
            return base;
        }
        Query text = Query.of(query -> query.multiMatch(match -> match
                .query(request.keyword().trim())
                .fields("name", "sku")));
        return Query.of(query -> query.bool(bool -> bool.must(text).filter(filters)));
    }

    public record ReindexResult(long indexed, String index) {
    }
}
