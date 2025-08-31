package com.tsb.banking.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String msg) { super(msg); }
}
