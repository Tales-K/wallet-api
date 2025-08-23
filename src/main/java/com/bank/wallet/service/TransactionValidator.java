package com.bank.wallet.service;

import com.bank.wallet.exception.InsufficientFundsException;
import com.bank.wallet.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionValidator {

	public void validateIdempotencyKey(String id) {
		if (id == null || id.trim().isEmpty()) throw new RuntimeException("Idempotency-Key header is required");
		if (id.length() > 200)
			throw new IllegalArgumentException("Idempotency-Key header exceeds maximum length of 200 characters");
	}

}

