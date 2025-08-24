package com.bank.wallet.service;

import com.bank.wallet.config.WalletProperties;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.exception.IdempotencyConflictException;
import com.bank.wallet.exception.IdempotencyInProgressException;
import com.bank.wallet.repository.IdempotencyKeyRepository;
import com.bank.wallet.util.ContextUtils;
import com.bank.wallet.util.SerializationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final ContextUtils contextUtils;
	private final SerializationUtils serializationUtils;
	private final WalletProperties walletProperties;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdempotencyKey claim(UUID idempotencyKey, Object requestDto) {
		var method = contextUtils.getCurrentRequestMethod();
		var path = contextUtils.getCurrentRequestPath();
		var requestHash = contextUtils.generateRequestHash(method, path, requestDto);
		var refId = UUID.randomUUID();
		var staleSeconds = walletProperties.getIdempotency().getStaleThresholdSeconds();

		var inserted = idempotencyKeyRepository.tryInsertWithRef(idempotencyKey, contextUtils.getCurrentRequestMethod(), contextUtils.getCurrentRequestPath(), requestHash, refId);
		if (inserted.isPresent()) return inserted.get();

		// cannot insert, so already exists
		var existing = idempotencyKeyRepository.findByIdempotencyKeyAndRequestHash(idempotencyKey, requestHash)
			.orElseThrow(() -> new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSE", "Idempotency key reused for different request"));

		// if in progress, try to take over if stale
		if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
			var taken = idempotencyKeyRepository.tryTakeOverReturning(idempotencyKey, staleSeconds);
			if (taken.isPresent()) return taken.get();
			throw new IdempotencyInProgressException("Request is being processed by another instance");
		}

		// if not in progress, then it's a replay
		return existing;
	}

	public boolean isReplay(IdempotencyKey key) {
		return key.getStatus() == IdempotencyStatus.SUCCEEDED || key.getStatus() == IdempotencyStatus.FAILED;
	}

	public ResponseEntity<String> buildReplayResponse(IdempotencyKey key) {
		return ResponseEntity.status(key.getResponseStatus()).body(key.getResponseBody());
	}

	public String markCompleted(IdempotencyKey key, int httpStatus, Object responseDto, IdempotencyStatus status) {
		try {
			var json = serializationUtils.toJson(responseDto);
			var rows = idempotencyKeyRepository.markCompleted(key.getIdempotencyKey(), httpStatus, json, status.name().toLowerCase(), key.getRequestHash());
			if (rows != 1) {
				log.error("Failed state transition to {} for key {} (hash mismatch or state)", status, key.getIdempotencyKey());
				throw new IllegalStateException("Idempotency key not in in_progress state or hash mismatch");
			}
			return json;
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to mark {} for key {}", status, key.getIdempotencyKey(), e);
			throw new RuntimeException("Failed to update idempotency status", e);
		}
	}

}
