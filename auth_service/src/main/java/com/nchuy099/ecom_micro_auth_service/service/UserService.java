package com.nchuy099.ecom_micro_auth_service.service;

import com.nchuy099.ecom_micro_auth_service.dto.request.LoginRequest;
import com.nchuy099.ecom_micro_auth_service.dto.request.UserRegistrationDto;
import com.nchuy099.ecom_micro_auth_service.dto.response.TokenResponse;

public interface UserService {

    void createUser(UserRegistrationDto dto);

    TokenResponse login(LoginRequest request);
}
