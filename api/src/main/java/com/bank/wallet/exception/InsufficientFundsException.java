package com.bank.wallet.exception;

import com.bank.wallet.entity.IdempotencyKey;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class InsufficientFundsException extends TransactionRuntimeException {
	private final UUID walletId;
	private final BigDecimal attemptedAmount;

	public InsufficientFundsException(String message, UUID walletId, BigDecimal attemptedAmount, IdempotencyKey idempotencyKey) {
		super(message, idempotencyKey);
		this.walletId = walletId;
		this.attemptedAmount = attemptedAmount;
	}
}
