package com.nchuy099.ecommerce.notification.controller;

import java.util.List;

import com.nchuy099.ecommerce.common.ApiResponse;
import com.nchuy099.ecommerce.notification.dto.NotificationDlqResponse;
import com.nchuy099.ecommerce.notification.dto.NotificationReplayResponse;
import com.nchuy099.ecommerce.notification.service.NotificationDlqService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/dlq")
public class NotificationAdminController {
    private final NotificationDlqService dlqService;

    public NotificationAdminController(NotificationDlqService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping
    public ApiResponse<List<NotificationDlqResponse>> findAll() {
        return ApiResponse.of(dlqService.findAll());
    }

    @PostMapping("/{id}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<NotificationReplayResponse> replay(@PathVariable Long id) {
        return ApiResponse.of(dlqService.replay(id));
    }
}
