package com.bank.wallet.service;

import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.exception.IdempotencyConflictException;
import com.bank.wallet.exception.IdempotencyInProgressException;
import com.bank.wallet.repository.IdempotencyKeyRepository;
import com.bank.wallet.util.ContextUtils;
import com.bank.wallet.util.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.bank.wallet.entity.enums.IdempotencyStatus.SUCCEEDED;
import static com.bank.wallet.service.IdempotencyService.STALE_THRESHOLD_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

	@Mock
	private IdempotencyKeyRepository repository;
	@Mock
	private ContextUtils contextUtils;
	@Mock
	private SerializationUtils serializationUtils;
	@InjectMocks
	private IdempotencyService service;

	private final String method = "POST";
	private final String path = "/transfers";
	private final String requestHash = "abc123";

	@Test
	void claim_insertsNewKey() {
		// arrange
		var key = UUID.randomUUID();
		var inserted = IdempotencyKey.builder().idempotencyKey(key).status(IdempotencyStatus.IN_PROGRESS).requestHash(requestHash).refId(UUID.randomUUID()).build();
		when(contextUtils.getCurrentRequestMethod()).thenReturn(method);
		when(contextUtils.getCurrentRequestPath()).thenReturn(path);
		when(contextUtils.generateRequestHash(eq(method), eq(path), any())).thenReturn(requestHash);
		when(repository.tryInsertWithRef(eq(key), eq(method), eq(path), eq(requestHash), any())).thenReturn(Optional.of(inserted));
		// act
		var result = service.claim(key, new Object());
		// assert
		assertSame(inserted, result);
	}

	@Test
	void claim_throwsConflictWhenNotFoundAfterInsertFail() {
		// arrange
		var key = UUID.randomUUID();
		when(contextUtils.getCurrentRequestMethod()).thenReturn(method);
		when(contextUtils.getCurrentRequestPath()).thenReturn(path);
		when(contextUtils.generateRequestHash(eq(method), eq(path), any())).thenReturn(requestHash);
		when(repository.tryInsertWithRef(eq(key), eq(method), eq(path), eq(requestHash), any())).thenReturn(Optional.empty());
		when(repository.findByIdempotencyKeyAndRequestHash(key, requestHash)).thenReturn(Optional.empty());
		// act & assert
		assertThrows(IdempotencyConflictException.class, () -> service.claim(key, new Object()));
	}

	@Test
	void claim_takesOverStaleInProgress() {
		// arrange
		var key = UUID.randomUUID();
		var existing = IdempotencyKey.builder().idempotencyKey(key).status(IdempotencyStatus.IN_PROGRESS).requestHash(requestHash).refId(UUID.randomUUID()).build();
		var takeover = IdempotencyKey.builder().idempotencyKey(key).status(IdempotencyStatus.IN_PROGRESS).requestHash(requestHash).refId(UUID.randomUUID()).build();
		when(contextUtils.getCurrentRequestMethod()).thenReturn(method);
		when(contextUtils.getCurrentRequestPath()).thenReturn(path);
		when(contextUtils.generateRequestHash(eq(method), eq(path), any())).thenReturn(requestHash);
		when(repository.tryInsertWithRef(eq(key), eq(method), eq(path), eq(requestHash), any())).thenReturn(Optional.empty());
		when(repository.findByIdempotencyKeyAndRequestHash(key, requestHash)).thenReturn(Optional.of(existing));
		when(repository.tryTakeOverReturning(key, STALE_THRESHOLD_SECONDS)).thenReturn(Optional.of(takeover));
		// act
		var result = service.claim(key, new Object());
		// assert
		assertSame(takeover, result);
	}

	@Test
	void claim_throwsInProgressWhenNotStale() {
		// arrange
		var key = UUID.randomUUID();
		var existing = IdempotencyKey.builder().idempotencyKey(key).status(IdempotencyStatus.IN_PROGRESS).requestHash(requestHash).refId(UUID.randomUUID()).build();
		when(contextUtils.getCurrentRequestMethod()).thenReturn(method);
		when(contextUtils.getCurrentRequestPath()).thenReturn(path);
		when(contextUtils.generateRequestHash(eq(method), eq(path), any())).thenReturn(requestHash);
		when(repository.tryInsertWithRef(eq(key), eq(method), eq(path), eq(requestHash), any())).thenReturn(Optional.empty());
		when(repository.findByIdempotencyKeyAndRequestHash(key, requestHash)).thenReturn(Optional.of(existing));
		when(repository.tryTakeOverReturning(key, STALE_THRESHOLD_SECONDS)).thenReturn(Optional.empty());
		// act & assert
		assertThrows(IdempotencyInProgressException.class, () -> service.claim(key, new Object()));
	}

	@Test
	void markCompleted_updatesSuccessfully() {
		// arrange
		var key = UUID.randomUUID();
		var existing = IdempotencyKey.builder().idempotencyKey(key).requestHash(requestHash).status(IdempotencyStatus.IN_PROGRESS).build();
		var responseObj = new Object();
		var json = "{\"ok\":true}";
		when(serializationUtils.toJson(responseObj)).thenReturn(json);
		when(repository.markCompleted(key, 200, json, SUCCEEDED.name().toLowerCase(), requestHash)).thenReturn(1);
		// act
		var result = service.markCompleted(existing, 200, responseObj, SUCCEEDED);
		// assert
		assertEquals(json, result);
	}

	@Test
	void markCompleted_throwsWhenRowNotUpdated() {
		// arrange
		var key = UUID.randomUUID();
		var existing = IdempotencyKey.builder().idempotencyKey(key).requestHash(requestHash).status(IdempotencyStatus.IN_PROGRESS).build();
		when(serializationUtils.toJson(any())).thenReturn("{}");
		when(repository.markCompleted(key, 200, "{}", SUCCEEDED.name().toLowerCase(), requestHash)).thenReturn(0);
		// act & assert
		assertThrows(IllegalStateException.class, () -> service.markCompleted(existing, 200, new Object(), SUCCEEDED));
	}
}
