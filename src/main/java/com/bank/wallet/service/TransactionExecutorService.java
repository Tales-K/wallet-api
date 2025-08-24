package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.entity.enums.IdempotencyStatus;
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
public class TransactionExecutorService {

	private final WalletService walletService;
	private final LedgerService ledgerService;
	private final IdempotencyService idempotencyService;
	private final TransactionMapper transactionMapper;

	@Transactional
	public ResponseEntity<String> deposit(String idempotencyKey, UUID txId, UUID walletId, TransactionRequestDto request) {
		try {
			var newBalance = walletService.depositAndGetNewBalance(walletId, request.getAmount());
			ledgerService.createDepositEntry(txId, walletId, request.getAmount());
			var responseDto = transactionMapper.toResponseDto(txId, walletId, newBalance);
			var body = idempotencyService.markCompleted(idempotencyKey, 200, responseDto, IdempotencyStatus.SUCCEEDED);
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			log.error("Error processing deposit for wallet: {}", walletId, e);
			throw e;
		}
	}

	@Transactional
	public ResponseEntity<String> withdraw(String idempotencyKey, UUID txId, UUID walletId, TransactionRequestDto request) {
		try {
			var newBalance = walletService.withdrawAndGetNewBalance(walletId, request.getAmount());
			ledgerService.createWithdrawEntry(txId, walletId, request.getAmount());
			var responseDto = transactionMapper.toResponseDto(txId, walletId, newBalance);
			var body = idempotencyService.markCompleted(idempotencyKey, 200, responseDto, IdempotencyStatus.SUCCEEDED);
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			log.error("Error processing withdrawal for wallet: {}", walletId, e);
			throw e;
		}
	}
}
