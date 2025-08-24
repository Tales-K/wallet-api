package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.mapper.TransferMapper;
import com.bank.wallet.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferExecutorService {

	private final WalletService walletService;
	private final LedgerService ledgerService;
	private final IdempotencyService idempotencyService;
	private final TransferMapper transferMapper;
	private final TransferRepository transferRepository;

	@Transactional
	public ResponseEntity<String> execute(IdempotencyKey idempotencyKey, TransferRequestDto request) {
		var transferId = idempotencyKey.getRefId();
		var amount = request.getAmount();
		var from = request.getFromWalletId();
		var to = request.getToWalletId();

		var fromNewBalance = walletService.withdrawAndGetNewBalance(idempotencyKey, from, amount);
		var toNewBalance = walletService.depositAndGetNewBalance(idempotencyKey, to, amount);

		ledgerService.createTransferDebitEntry(transferId, from, amount, fromNewBalance);
		ledgerService.createTransferCreditEntry(transferId, to, amount, toNewBalance);
		var rows = transferRepository.insertIfAbsent(transferId, from, to, amount);
		if (rows != 1) {
			log.error("Transfer insert failed transferId={} from={} to={} rows={}", transferId, from, to, rows);
			throw new IllegalStateException("Transfer insertion failed");
		}

		var responseDto = transferMapper.toResponse(transferId, from, to, amount);
		var body = idempotencyService.markCompleted(idempotencyKey, 200, responseDto, IdempotencyStatus.SUCCEEDED);
		return ResponseEntity.ok(body);
	}
}
