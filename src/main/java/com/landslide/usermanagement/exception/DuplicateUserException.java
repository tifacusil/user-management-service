package com.landslide.usermanagement.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) { super(message); }
}
