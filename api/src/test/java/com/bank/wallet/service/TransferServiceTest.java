package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.validator.TransactionValidator;
import com.bank.wallet.validator.TransferValidator;
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
class TransferServiceTest {

	@Mock
	private IdempotencyService idempotencyService;
	@Mock
	private TransferExecutorService transferExecutorService;
	@Mock
	private TransactionValidator transactionValidator;
	@Mock
	private TransferValidator transferValidator;

	@InjectMocks
	private TransferService transferService;

	@Test
	void create_processesRequest() {
		// arrange
		var from = UUID.randomUUID();
		var to = UUID.randomUUID();
		var amount = new BigDecimal("12.34");
		var req = TransferRequestDto.builder().fromWalletId(from).toWalletId(to).amount(amount).build();
		var idKey = UUID.randomUUID();
		var key = IdempotencyKey.builder().idempotencyKey(idKey).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		when(idempotencyService.claim(idKey, req)).thenReturn(key);
		when(idempotencyService.isReplay(key)).thenReturn(false);
		when(transferExecutorService.execute(key, req)).thenReturn(ResponseEntity.ok("body"));
		// act
		var response = transferService.create(req, idKey);
		// assert
		verify(transactionValidator).validateIdempotencyKey(idKey);
		verify(transferValidator).validate(req, key);
		assertEquals(200, response.getStatusCode().value());
		assertEquals("body", response.getBody());
	}

	@Test
	void create_returnsReplay() {
		// arrange
		var from = UUID.randomUUID();
		var to = UUID.randomUUID();
		var amount = new BigDecimal("5.00");
		var req = TransferRequestDto.builder().fromWalletId(from).toWalletId(to).amount(amount).build();
		var idKey = UUID.randomUUID();
		var key = IdempotencyKey.builder().idempotencyKey(idKey).refId(UUID.randomUUID()).status(IdempotencyStatus.SUCCEEDED).responseStatus(200).responseBody("cached").build();
		when(idempotencyService.claim(idKey, req)).thenReturn(key);
		when(idempotencyService.isReplay(key)).thenReturn(true);
		when(idempotencyService.buildReplayResponse(key)).thenReturn(ResponseEntity.ok("cached"));
		// act
		var response = transferService.create(req, idKey);
		// assert
		verify(transactionValidator).validateIdempotencyKey(idKey);
		verify(transferValidator).validate(req, key);
		assertEquals("cached", response.getBody());
	}
}
