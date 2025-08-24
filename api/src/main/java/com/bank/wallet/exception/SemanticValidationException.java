package com.bank.wallet.exception;

import com.bank.wallet.entity.IdempotencyKey;

public class SemanticValidationException extends TransactionRuntimeException {
	public SemanticValidationException(String message, IdempotencyKey idempotencyKey) {
		super(message, idempotencyKey);
	}
}

