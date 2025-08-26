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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

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
		log.info("Transfer: from={}, to={}, amount={}", from, to, amount);

		var balanceByWallet = new HashMap<UUID, BigDecimal>(2);

		var operations = Stream.of(
			new WalletOperation(from, walletId -> walletService.withdrawAndGetNewBalance(idempotencyKey, walletId, amount)),
			new WalletOperation(to, walletId -> walletService.depositAndGetNewBalance(idempotencyKey, walletId, amount))
		);

		// run withdraw or deposit first based on walletId to prevent deadlocks
		operations.sorted(Comparator.comparing(WalletOperation::walletId))
			.forEach(op -> balanceByWallet.put(op.walletId(), op.action().apply(op.walletId())));

		ledgerService.createTransferDebitEntry(transferId, from, amount, balanceByWallet.get(from));
		ledgerService.createTransferCreditEntry(transferId, to, amount, balanceByWallet.get(to));

		var rows = transferRepository.insertIfAbsent(transferId, from, to, amount);
		if (rows != 1) {
			log.error("Transfer insert failed transferId={} from={} to={} rows={}", transferId, from, to, rows);
			throw new IllegalStateException("Transfer insertion failed");
		}

		var responseDto = transferMapper.toResponse(transferId, from, to, amount);
		var body = idempotencyService.markCompleted(idempotencyKey, 200, responseDto, IdempotencyStatus.SUCCEEDED);
		return ResponseEntity.ok(body);
	}

	record WalletOperation(UUID walletId, Function<UUID, BigDecimal> action) {
	}

}
