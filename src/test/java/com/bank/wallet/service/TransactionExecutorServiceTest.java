package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.exception.InsufficientFundsException;
import com.bank.wallet.exception.WalletNotFoundException;
import com.bank.wallet.mapper.TransactionMapper;
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
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionExecutorServiceTest {

	@Mock
	private WalletService walletService;
	@Mock
	private LedgerService ledgerService;
	@Mock
	private IdempotencyService idempotencyService;
	@Spy
	private TransactionMapper transactionMapper = new TransactionMapper();
	@InjectMocks
	private TransactionExecutorService executor;

	@Test
	void deposit_processesRequest() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("12.34");
		var request = TransactionRequestDto.builder().amount(amount).build();
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var newBalance = new BigDecimal("100.00");
		when(walletService.depositAndGetNewBalance(key, walletId, amount)).thenReturn(newBalance);
		when(idempotencyService.markCompleted(eq(key), eq(200), any(), eq(IdempotencyStatus.SUCCEEDED))).thenReturn("{json}");
		// act
		var response = executor.deposit(key, walletId, request);
		// assert
		assertEquals(200, response.getStatusCode().value());
		assertEquals("{json}", response.getBody());
		verify(ledgerService).createDepositEntry(key.getRefId(), walletId, amount, newBalance);
	}

	@Test
	void deposit_throwsWalletNotFound() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("5.00");
		var request = TransactionRequestDto.builder().amount(amount).build();
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		when(walletService.depositAndGetNewBalance(key, walletId, amount)).thenThrow(new WalletNotFoundException("wallet not found", key));
		// act
		// assert
		assertThrows(WalletNotFoundException.class, () -> executor.deposit(key, walletId, request));
		verify(ledgerService, never()).createDepositEntry(any(), any(), any(), any());
		verify(idempotencyService, never()).markCompleted(any(), anyInt(), any(), any());
	}

	@Test
	void withdraw_processesRequest() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("20.00");
		var request = TransactionRequestDto.builder().amount(amount).build();
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var newBalance = new BigDecimal("80.00");
		when(walletService.withdrawAndGetNewBalance(key, walletId, amount)).thenReturn(newBalance);
		when(idempotencyService.markCompleted(eq(key), eq(200), any(), eq(IdempotencyStatus.SUCCEEDED))).thenReturn("{json-w}");
		// act
		var response = executor.withdraw(key, walletId, request);
		// assert
		assertEquals(200, response.getStatusCode().value());
		assertEquals("{json-w}", response.getBody());
		verify(ledgerService).createWithdrawEntry(key.getRefId(), walletId, amount, newBalance);
	}

	@Test
	void withdraw_throwsInsufficientFunds() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("50.00");
		var request = TransactionRequestDto.builder().amount(amount).build();
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		when(walletService.withdrawAndGetNewBalance(key, walletId, amount)).thenThrow(new InsufficientFundsException("insufficient", walletId, amount, key));
		// act
		// assert
		assertThrows(InsufficientFundsException.class, () -> executor.withdraw(key, walletId, request));
		verify(ledgerService, never()).createWithdrawEntry(any(), any(), any(), any());
		verify(idempotencyService, never()).markCompleted(any(), anyInt(), any(), any());
	}
}
