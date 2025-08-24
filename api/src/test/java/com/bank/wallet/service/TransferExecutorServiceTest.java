package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.exception.InsufficientFundsException;
import com.bank.wallet.mapper.TransferMapper;
import com.bank.wallet.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferExecutorServiceTest {

	@Mock
	private WalletService walletService;
	@Mock
	private LedgerService ledgerService;
	@Mock
	private IdempotencyService idempotencyService;
	@Mock
	private TransferRepository transferRepository;
	@Spy
	private TransferMapper transferMapper = new TransferMapper();
	@InjectMocks
	private TransferExecutorService executorService;

	@Test
	void execute_processesTransfer() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var from = UUID.randomUUID();
		var to = UUID.randomUUID();
		var amount = new BigDecimal("42.555");
		var req = TransferRequestDto.builder().fromWalletId(from).toWalletId(to).amount(amount).build();
		when(walletService.withdrawAndGetNewBalance(key, from, amount)).thenReturn(new BigDecimal("100.00"));
		when(walletService.depositAndGetNewBalance(key, to, amount)).thenReturn(new BigDecimal("200.00"));
		when(transferRepository.insertIfAbsent(key.getRefId(), from, to, amount)).thenReturn(1);
		when(idempotencyService.markCompleted(eq(key), eq(200), any(), eq(IdempotencyStatus.SUCCEEDED))).thenReturn("{json}");
		// act
		var response = executorService.execute(key, req);
		// assert
		assertEquals(200, response.getStatusCode().value());
		assertEquals("{json}", response.getBody());
	}

	@Test
	void execute_throwsWhenNoRowInserted() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var from = UUID.randomUUID();
		var to = UUID.randomUUID();
		var amount = new BigDecimal("10.00");
		var req = TransferRequestDto.builder().fromWalletId(from).toWalletId(to).amount(amount).build();
		when(walletService.withdrawAndGetNewBalance(key, from, amount)).thenReturn(new BigDecimal("90.00"));
		when(walletService.depositAndGetNewBalance(key, to, amount)).thenReturn(new BigDecimal("110.00"));
		when(transferRepository.insertIfAbsent(key.getRefId(), from, to, amount)).thenReturn(0);
		// act & assert
		assertThrows(IllegalStateException.class, () -> executorService.execute(key, req));
	}

	@Test
	void execute_throwsInsufficientFundsException() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var from = UUID.randomUUID();
		var to = UUID.randomUUID();
		var amount = new BigDecimal("42.555");
		var req = TransferRequestDto.builder().fromWalletId(from).toWalletId(to).amount(amount).build();
		var ex = mock(InsufficientFundsException.class);
		when(walletService.withdrawAndGetNewBalance(key, from, amount)).thenThrow(ex);
		// act & assert
		assertThrows(InsufficientFundsException.class, () -> executorService.execute(key, req));
	}

}
