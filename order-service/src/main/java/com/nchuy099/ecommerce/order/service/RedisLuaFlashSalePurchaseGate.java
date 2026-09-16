package com.nchuy099.ecommerce.order.service;

import java.util.List;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisLuaFlashSalePurchaseGate implements FlashSalePurchaseGate {
    private static final RedisScript<Long> PURCHASE_SCRIPT = new DefaultRedisScript<>("""
            local quantity = tonumber(ARGV[1])
            local max_per_user = tonumber(ARGV[2])
            local purchased = tonumber(redis.call("get", KEYS[2]) or "0")
            if purchased + quantity > max_per_user then
                return 2
            end
            local stock = tonumber(redis.call("get", KEYS[1]) or "0")
            if stock < quantity then
                return 1
            end
            redis.call("decrby", KEYS[1], quantity)
            redis.call("incrby", KEYS[2], quantity)
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public FlashSalePurchaseResult tryPurchase(FlashSaleCampaignEntity campaign, Long userId, int quantity) {
        redisTemplate.opsForValue().setIfAbsent(stockKey(campaign.getId()), String.valueOf(campaign.getStock()));
        Long result = redisTemplate.execute(
                PURCHASE_SCRIPT,
                List.of(stockKey(campaign.getId()), userKey(campaign.getId(), userId)),
                String.valueOf(quantity),
                String.valueOf(campaign.getMaxPerUser())
        );
        if (result == null) {
            throw new IllegalStateException("Redis Lua purchase script returned no result");
        }
        return switch (result.intValue()) {
            case 0 -> FlashSalePurchaseResult.ACCEPTED;
            case 1 -> FlashSalePurchaseResult.SOLD_OUT;
            case 2 -> FlashSalePurchaseResult.ALREADY_PURCHASED;
            default -> throw new IllegalStateException("Unknown flash sale Lua result: " + result);
        };
    }

    static String stockKey(Long campaignId) {
        return "flashsale:campaign:" + campaignId + ":stock";
    }

    static String userKey(Long campaignId, Long userId) {
        return "flashsale:campaign:" + campaignId + ":user:" + userId;
    }
}
