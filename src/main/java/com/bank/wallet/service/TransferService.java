package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

	private final IdempotencyService idempotencyService;
	private final TransferExecutorService transferExecutorService;
	private final TransactionValidator transactionValidator;
	private final TransferValidator transferValidator;

	public ResponseEntity<String> create(TransferRequestDto request, UUID idempotencyKey) {
		transactionValidator.validateIdempotencyKey(idempotencyKey);
		var key = idempotencyService.claim(idempotencyKey, request);
		transferValidator.validate(request, key);
		if (idempotencyService.isReplay(key)) return idempotencyService.buildReplayResponse(key);
		return transferExecutorService.execute(key, request);
	}

}
