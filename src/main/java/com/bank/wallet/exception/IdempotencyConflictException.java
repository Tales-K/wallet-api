package com.bank.wallet.exception;

import lombok.Getter;

@Getter
public class IdempotencyConflictException extends RuntimeException {
    private final String code;

    public IdempotencyConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
