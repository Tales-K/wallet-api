package com.bank.wallet.exception;

import com.bank.wallet.entity.IdempotencyKey;
import lombok.Getter;

@Getter
public abstract class TransactionRuntimeException extends RuntimeException {
	private final IdempotencyKey idempotencyKey;

	public TransactionRuntimeException(String message) {
		super(message);
		this.idempotencyKey = null;
	}

	public TransactionRuntimeException(String message, IdempotencyKey idempotencyKey) {
		super(message);
		this.idempotencyKey = idempotencyKey;
	}
}
