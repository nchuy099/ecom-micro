package com.nchuy099.ecommerce.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecommerce.search")
public record ProductSearchProperties(
        String backend,
        String indexAlias,
        int reindexBatchSize
) {
    public ProductSearchProperties {
        backend = backend == null || backend.isBlank() ? "elasticsearch" : backend;
        indexAlias = indexAlias == null || indexAlias.isBlank() ? "products" : indexAlias;
        if (reindexBatchSize <= 0) {
            reindexBatchSize = 250;
        }
    }
}
