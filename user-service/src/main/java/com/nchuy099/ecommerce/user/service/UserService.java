package com.nchuy099.ecommerce.user.service;

import com.nchuy099.ecommerce.user.dto.CreateUserRequest;
import com.nchuy099.ecommerce.user.dto.PageResponse;
import com.nchuy099.ecommerce.user.dto.UpdateUserRequest;
import com.nchuy099.ecommerce.user.dto.UserResponse;
import com.nchuy099.ecommerce.user.dto.UserSearchRequest;
import com.nchuy099.ecommerce.user.entity.UserEntity;
import com.nchuy099.ecommerce.user.exception.DuplicateUserException;
import com.nchuy099.ecommerce.user.exception.UserNotFoundException;
import com.nchuy099.ecommerce.user.repository.UserRepository;
import com.nchuy099.ecommerce.user.specification.UserSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        try {
            UserEntity user = new UserEntity(
                    request.username(),
                    request.email(),
                    request.phone(),
                    request.role(),
                    request.tier()
            );
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUserException();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setTier(request.tier());
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUserException();
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(UserSearchRequest request) {
        Specification<UserEntity> specification = Specification.allOf(
                UserSpecifications.keywordContains(request.keyword()),
                UserSpecifications.hasRole(request.role()),
                UserSpecifications.hasTier(request.tier())
        );
        PageRequest pageRequest = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<UserEntity> page = userRepository.findAll(specification, pageRequest);
        return PageResponse.of(
                page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
