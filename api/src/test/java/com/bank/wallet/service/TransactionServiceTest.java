package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.validator.TransactionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private IdempotencyService idempotencyService;
	@Mock
	private TransactionValidator transactionValidator;
	@Mock
	private TransactionExecutorService transactionExecutorService;
	@InjectMocks
	private TransactionService transactionService;

	@Test
	void deposit_processesRequest() {
		// arrange
		var walletId = UUID.randomUUID();
		var idKey = UUID.randomUUID();
		var request = TransactionRequestDto.builder().amount(new BigDecimal("12.34")).build();
		var keyEntity = IdempotencyKey.builder().idempotencyKey(idKey).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		when(idempotencyService.claim(idKey, request)).thenReturn(keyEntity);
		when(idempotencyService.isReplay(keyEntity)).thenReturn(false);
		when(transactionExecutorService.deposit(keyEntity, walletId, request)).thenReturn(ResponseEntity.ok("body"));
		// act
		var response = transactionService.deposit(walletId, request, idKey);
		// assert
		verify(transactionValidator).validateIdempotencyKey(idKey);
		assertEquals(200, response.getStatusCode().value());
		assertEquals("body", response.getBody());
	}

	@Test
	void deposit_returnsReplay() {
		// arrange
		var walletId = UUID.randomUUID();
		var idKey = UUID.randomUUID();
		var request = TransactionRequestDto.builder().amount(new BigDecimal("5.00")).build();
		var keyEntity = IdempotencyKey.builder().idempotencyKey(idKey).refId(UUID.randomUUID()).status(IdempotencyStatus.SUCCEEDED).responseStatus(200).responseBody("cached").build();
		when(idempotencyService.claim(idKey, request)).thenReturn(keyEntity);
		when(idempotencyService.isReplay(keyEntity)).thenReturn(true);
		when(idempotencyService.buildReplayResponse(keyEntity)).thenReturn(ResponseEntity.ok("cached"));
		// act
		var response = transactionService.deposit(walletId, request, idKey);
		// assert
		verify(transactionValidator).validateIdempotencyKey(idKey);
		assertEquals("cached", response.getBody());
	}

	@Test
	void withdraw_processesRequest() {
		// arrange
		var walletId = UUID.randomUUID();
		var idKey = UUID.randomUUID();
		var request = TransactionRequestDto.builder().amount(new BigDecimal("8.00")).build();
		var keyEntity = IdempotencyKey.builder().idempotencyKey(idKey).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		when(idempotencyService.claim(idKey, request)).thenReturn(keyEntity);
		when(idempotencyService.isReplay(keyEntity)).thenReturn(false);
		when(transactionExecutorService.withdraw(keyEntity, walletId, request)).thenReturn(ResponseEntity.ok("body-w"));
		// act
		var response = transactionService.withdraw(walletId, request, idKey);
		// assert
		verify(transactionValidator).validateIdempotencyKey(idKey);
		assertEquals(200, response.getStatusCode().value());
		assertEquals("body-w", response.getBody());
	}

	@Test
	void withdraw_returnsReplay() {
		// arrange
		var walletId = UUID.randomUUID();
		var idKey = UUID.randomUUID();
		var request = TransactionRequestDto.builder().amount(new BigDecimal("3.50")).build();
		var keyEntity = IdempotencyKey.builder().idempotencyKey(idKey).refId(UUID.randomUUID()).status(IdempotencyStatus.SUCCEEDED).responseStatus(200).responseBody("cached-w").build();
		when(idempotencyService.claim(idKey, request)).thenReturn(keyEntity);
		when(idempotencyService.isReplay(keyEntity)).thenReturn(true);
		when(idempotencyService.buildReplayResponse(keyEntity)).thenReturn(ResponseEntity.ok("cached-w"));
		// act
		var response = transactionService.withdraw(walletId, request, idKey);
		// assert
		verify(transactionValidator).validateIdempotencyKey(idKey);
		assertEquals("cached-w", response.getBody());
	}
}

