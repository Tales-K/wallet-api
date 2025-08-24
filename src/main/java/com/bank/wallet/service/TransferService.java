package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

	private final IdempotencyService idempotencyService;
	private final TransferExecutorService transferExecutorService;
	private final TransactionValidator transactionValidator;
	private final TransferValidator transferValidator;

	public ResponseEntity<String> create(TransferRequestDto request, String idempotencyKey) {
		transactionValidator.validateIdempotencyKey(idempotencyKey);
		transferValidator.validate(request);
		var claim = idempotencyService.claim(idempotencyKey, request);
		if (claim.cachedResponse() != null) return claim.cachedResponse();
		var transferId = claim.refId();
		return transferExecutorService.execute(idempotencyKey, transferId, request);
	}
}
