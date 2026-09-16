package com.nchuy099.ecommerce.order.service;

final class NoopFlashSaleDistributedLock implements FlashSaleDistributedLock {
    static final NoopFlashSaleDistributedLock INSTANCE = new NoopFlashSaleDistributedLock();

    private NoopFlashSaleDistributedLock() {
    }

    @Override
    public String tryLock(Long campaignId) {
        return "unit-test-lock";
    }

    @Override
    public void unlock(Long campaignId, String token) {
    }
}
