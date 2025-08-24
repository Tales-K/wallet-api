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
	private static final int STALE_THRESHOLD_SECONDS = 30;

	public record IdempotencyClaimResult(ResponseEntity<String> cachedResponse, UUID refId, String requestHash) {
		public static IdempotencyClaimResult replay(ResponseEntity<String> r) { return new IdempotencyClaimResult(r, null, null); }
		public static IdempotencyClaimResult claimed(UUID refId, String hash) { return new IdempotencyClaimResult(null, refId, hash); }
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdempotencyClaimResult claim(String idempotencyKey, Object requestDto) {
		var method = contextUtils.getCurrentRequestMethod();
		var path = contextUtils.getCurrentRequestPath();
		var requestHash = contextUtils.generateRequestHash(method, path, requestDto);
		var refId = UUID.randomUUID();
		var inserted = idempotencyKeyRepository.insertNewWithRef(idempotencyKey, method, path, requestHash, refId);
		if (inserted == 1) return IdempotencyClaimResult.claimed(refId, requestHash);
		var existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
			.orElseThrow(() -> new RuntimeException("Idempotency key not found after conflict"));
		return handleExisting(existing, requestHash);
	}

	private IdempotencyClaimResult handleExisting(IdempotencyKey existing, String requestHash) {
		if (!requestHash.equals(existing.getRequestHash())) {
			throw new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSE", "Idempotency key reused for different request");
		}
		if (existing.getStatus() == IdempotencyStatus.SUCCEEDED || existing.getStatus() == IdempotencyStatus.FAILED) {
			if (existing.getResponseBody() == null) {
				log.warn("Idempotency key stored without response body: {}", existing.getIdempotencyKey());
				throw new RuntimeException("Cached response missing for idempotency key");
			}
			return IdempotencyClaimResult.replay(ResponseEntity.status(existing.getResponseStatus()).body(existing.getResponseBody()));
		}
		if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
			var updated = idempotencyKeyRepository.tryTakeOver(existing.getIdempotencyKey(), STALE_THRESHOLD_SECONDS);
			if (updated == 1) {
				return IdempotencyClaimResult.claimed(existing.getRefId(), existing.getRequestHash());
			}
			throw new IdempotencyInProgressException("Request is being processed by another instance");
		}
		return IdempotencyClaimResult.claimed(existing.getRefId(), existing.getRequestHash());
	}

	public String markCompleted(String idempotencyKey, int httpStatus, Object responseDto, IdempotencyStatus status, String requestHash) {
		try {
			var json = serializationUtils.toJson(responseDto);
			var storedBodyOpt = idempotencyKeyRepository.markCompleted(idempotencyKey, httpStatus, json, status.getValue(), requestHash);
			if (storedBodyOpt.isEmpty()) {
				log.error("Failed state transition to {} for key {} (hash mismatch or state)", status, idempotencyKey);
				throw new IllegalStateException("Idempotency key not in in_progress state or hash mismatch");
			}
			return storedBodyOpt.get();
		} catch (Exception e) {
			log.error("Failed to mark {} for key {}", status, idempotencyKey, e);
			throw new RuntimeException("Failed to update idempotency status", e);
		}
	}
}
