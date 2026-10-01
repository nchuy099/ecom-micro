package com.nchuy099.ecom_micro_order_service.service;

import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;

public interface OrderService {
    OrderEntity create(CreateOrderReq req);

    void updateOrderStatusLocked(String orderId);
}
