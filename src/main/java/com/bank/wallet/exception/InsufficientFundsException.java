package com.bank.wallet.exception;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class InsufficientFundsException extends RuntimeException {
    private final UUID walletId;
    private final BigDecimal attemptedAmount;

    public InsufficientFundsException(String message, UUID walletId, BigDecimal attemptedAmount) {
        super(message);
        this.walletId = walletId;
        this.attemptedAmount = attemptedAmount;
    }
}
