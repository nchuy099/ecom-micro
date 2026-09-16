package com.nchuy099.ecommerce.order.service;

public interface FlashSaleDistributedLock {
    String tryLock(Long campaignId);

    void unlock(Long campaignId, String token);
}
