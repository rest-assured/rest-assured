package com.jayway.restassured.exception;

public class AssertionFailedException extends RuntimeException {
    public AssertionFailedException(String message) {
        super(message);
    }
}
