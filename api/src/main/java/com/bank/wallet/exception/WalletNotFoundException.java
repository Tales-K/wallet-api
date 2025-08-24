package com.bank.wallet.exception;

import com.bank.wallet.entity.IdempotencyKey;
import lombok.Getter;

@Getter
public class WalletNotFoundException extends TransactionRuntimeException {

	public WalletNotFoundException(String message) {
		super(message);
	}

	public WalletNotFoundException(String message, IdempotencyKey idempotencyKey) {
		super(message, idempotencyKey);
	}

}
