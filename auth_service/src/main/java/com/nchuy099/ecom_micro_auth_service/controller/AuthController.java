package com.nchuy099.ecom_micro_auth_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nchuy099.ecom_micro_auth_service.dto.request.LoginRequest;
import com.nchuy099.ecom_micro_auth_service.dto.request.UserRegistrationDto;
import com.nchuy099.ecom_micro_auth_service.dto.response.TokenResponse;
import com.nchuy099.ecom_micro_auth_service.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationDto dto) {
        userService.createUser(dto);
        return ResponseEntity.ok("Người dùng đã được tạo thành công");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
