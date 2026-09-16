package com.nchuy099.ecommerce.user.controller;

import com.nchuy099.ecommerce.user.api.ApiResponse;
import com.nchuy099.ecommerce.user.dto.CreateUserRequest;
import com.nchuy099.ecommerce.user.dto.PageResponse;
import com.nchuy099.ecommerce.user.dto.UpdateUserRequest;
import com.nchuy099.ecommerce.user.dto.UserResponse;
import com.nchuy099.ecommerce.user.dto.UserSearchRequest;
import com.nchuy099.ecommerce.user.service.UserService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.of(userService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.of(userService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.of(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @PostMapping("/search")
    public ApiResponse<PageResponse<UserResponse>> search(@Valid @RequestBody UserSearchRequest request) {
        return ApiResponse.of(userService.search(request));
    }
}
