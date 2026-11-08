package com.nchuy099.ecom_micro_order_service.service;

import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;

public interface OrderService {
    OrderEntity create(CreateOrderReq req);

    void updateOrderStatusLocked(String orderId);
    void confirm(String orderId);
    void cancel(String orderId, String reason);
}
