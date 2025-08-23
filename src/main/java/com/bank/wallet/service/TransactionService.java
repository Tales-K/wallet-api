package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.mapper.TransactionMapper;
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

	private final WalletService walletService;
	private final LedgerService ledgerService;
	private final IdempotencyService idempotencyService;
	private final TransactionMapper transactionMapper;
	private final TransactionValidator validator;

	@Transactional
	public ResponseEntity<String> deposit(UUID walletId, TransactionRequestDto request, String idempotencyKey) {
		validator.validateIdempotencyKey(idempotencyKey);
		log.debug("Processing deposit for wallet: {}, amount: {}", walletId, request.getAmount());

		var cached = idempotencyService.checkCacheOrProceed(idempotencyKey, request);
		if (cached != null) return cached;

		try {
			var newBalance = walletService.depositAndGetNewBalance(walletId, request.getAmount());
			var txId = UUID.randomUUID();
			ledgerService.createDepositEntry(txId, walletId, request.getAmount());
			var responseDto = transactionMapper.toResponseDto(txId, walletId, newBalance);
			var body = idempotencyService.markCompleted(idempotencyKey, 200, responseDto);
			return ResponseEntity.status(200).body(body);
		} catch (Exception e) {
			log.error("Error processing deposit for wallet: {}", walletId, e);
			throw e;
		}
	}

	@Transactional
	public ResponseEntity<String> withdraw(UUID walletId, TransactionRequestDto request, String idempotencyKey) {
		validator.validateIdempotencyKey(idempotencyKey);
		log.debug("Processing withdrawal for wallet: {}, amount: {}", walletId, request.getAmount());

		var cached = idempotencyService.checkCacheOrProceed(idempotencyKey, request);
		if (cached != null) return cached;

		try {
			var newBalance = walletService.withdrawAndGetNewBalance(walletId, request.getAmount());
			var txId = UUID.randomUUID();
			ledgerService.createWithdrawEntry(txId, walletId, request.getAmount());
			var responseDto = transactionMapper.toResponseDto(txId, walletId, newBalance);
			var body = idempotencyService.markCompleted(idempotencyKey, 200, responseDto);
			return ResponseEntity.status(200).body(body);
		} catch (Exception e) {
			log.error("Error processing withdrawal for wallet: {}", walletId, e);
			throw e;
		}
	}
}
