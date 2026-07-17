package com.nchuy099.ecommerce.user.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException() {
        super("Username or email already exists");
    }
}
