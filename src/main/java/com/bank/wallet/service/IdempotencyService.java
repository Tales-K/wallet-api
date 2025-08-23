package com.bank.wallet.service;

import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.exception.IdempotencyConflictException;
import com.bank.wallet.exception.IdempotencyInProgressException;
import com.bank.wallet.repository.IdempotencyKeyRepository;
import com.bank.wallet.util.ContextUtils;
import com.bank.wallet.util.SerializationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final ContextUtils contextUtils;
	private final SerializationUtils serializationUtils;
	private static final int STALE_THRESHOLD_SECONDS = 30;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ResponseEntity<String> checkCacheOrProceed(String idempotencyKey, Object requestDto) {
		var method = contextUtils.getCurrentRequestMethod();
		var path = contextUtils.getCurrentRequestPath();
		var requestHash = contextUtils.generateRequestHash(method, path, requestDto);
		try {
			idempotencyKeyRepository.insertNew(idempotencyKey, method, path, requestHash);
			return null;
		} catch (DuplicateKeyException e) {
			var existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
				.orElseThrow(() -> new RuntimeException("Idempotency key not found after conflict"));
			return handleExisting(existing, requestHash);
		}
	}

	private ResponseEntity<String> handleExisting(IdempotencyKey existingKey, String requestHash) {
		if (!requestHash.equals(existingKey.getRequestHash())) {
			throw new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSE", "Idempotency key reused for different request");
		}
		if (existingKey.getStatus() == IdempotencyStatus.COMPLETED) {
			if (existingKey.getResponseBody() == null) {
				log.warn("Idempotency key stored without response body: {}", existingKey.getIdempotencyKey());
				throw new RuntimeException("Cached response missing for idempotency key");
			}
			return ResponseEntity.status(existingKey.getResponseStatus()).body(existingKey.getResponseBody());
		}
		if (existingKey.getStatus() == IdempotencyStatus.IN_PROGRESS) {
			var updated = idempotencyKeyRepository.tryTakeOver(existingKey.getIdempotencyKey(), STALE_THRESHOLD_SECONDS);
			if (updated == 1) {
				log.warn("Took over stale idempotency key {}", existingKey.getIdempotencyKey());
				return null;
			}
			throw new IdempotencyInProgressException("Request is being processed by another instance");
		}
		return null;
	}

	public String markCompleted(String idempotencyKey, int httpStatus, Object responseDto) {
		try {
			var canonical = serializationUtils.canonicalJson(responseDto);
			var updated = idempotencyKeyRepository.markCompleted(idempotencyKey, httpStatus, canonical);
			if (updated != 1) {
				log.error("Failed state transition to COMPLETED for key {} (updated={})", idempotencyKey, updated);
				throw new IllegalStateException("Idempotency key not in in_progress state");
			}
			log.debug("Idempotency key '{}' marked as COMPLETED", idempotencyKey);
			return canonical;
		} catch (Exception e) {
			log.error("Failed to mark COMPLETED for key {}", idempotencyKey, e);
			throw new RuntimeException("Failed to update idempotency status", e);
		}
	}
}
