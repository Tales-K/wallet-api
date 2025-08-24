package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

	private final IdempotencyService idempotencyService;
	private final TransactionValidator validator;
	private final TransactionExecutorService transactionExecutorService;

	public ResponseEntity<String> deposit(UUID walletId, TransactionRequestDto request, String idempotencyKey) {
		validator.validateIdempotencyKey(idempotencyKey);
		log.debug("Processing deposit for wallet: {}, amount: {}", walletId, request.getAmount());

		var cached = idempotencyService.checkCacheOrProceed(idempotencyKey, request);
		if (cached != null) return cached;

		return transactionExecutorService.deposit(walletId, request, idempotencyKey);
	}

	public ResponseEntity<String> withdraw(UUID walletId, TransactionRequestDto request, String idempotencyKey) {
		validator.validateIdempotencyKey(idempotencyKey);
		log.debug("Processing withdrawal for wallet: {}, amount: {}", walletId, request.getAmount());

		var cached = idempotencyService.checkCacheOrProceed(idempotencyKey, request);
		if (cached != null) return cached;

		return transactionExecutorService.withdraw(walletId, request, idempotencyKey);
	}
}
