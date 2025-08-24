package com.bank.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionValidator {

	public void validateIdempotencyKey(UUID id) {
		if (id == null) throw new RuntimeException("Idempotency-Key header is required");
	}

}

