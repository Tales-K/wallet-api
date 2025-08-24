package com.bank.wallet.service;

import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.Wallet;
import com.bank.wallet.entity.enums.PostingType;
import com.bank.wallet.exception.InsufficientFundsException;
import com.bank.wallet.exception.WalletNotFoundException;
import com.bank.wallet.mapper.WalletMapper;
import com.bank.wallet.repository.WalletRepository;
import com.bank.wallet.validator.WalletValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

	@Mock
	private WalletRepository walletRepository;
	@Mock
	private LedgerService ledgerService;
	@Mock
	private WalletValidator walletValidator;
	@Spy
	private WalletMapper walletMapper = new WalletMapper();
	@InjectMocks
	private WalletService walletService;

	@Test
	void createWallet_createsWallet() {
		// arrange
		var saved = Wallet.builder()
			.walletId(UUID.randomUUID())
			.currentBalance(BigDecimal.ZERO)
			.createdAt(OffsetDateTime.now())
			.updatedAt(OffsetDateTime.now())
			.build();
		when(walletRepository.save(any())).thenReturn(saved);
		// act
		var dto = walletService.createWallet();
		// assert
		assertEquals(saved.getWalletId(), dto.getWalletId());
		assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), dto.getCurrentBalance());
	}

	@Test
	void depositAndGetNewBalance_returnsNewBalance() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("25.50");
		when(walletRepository.depositAndGetNewBalance(walletId, amount)).thenReturn(Optional.of(new BigDecimal("100.75")));
		var idk = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).build();
		// act
		var result = walletService.depositAndGetNewBalance(idk, walletId, amount);
		// assert
		assertEquals(new BigDecimal("100.75"), result);
	}

	@Test
	void depositAndGetNewBalance_throwsWhenWalletMissing() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("10.00");
		when(walletRepository.depositAndGetNewBalance(walletId, amount)).thenReturn(Optional.empty());
		var idk = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).build();
		// act & assert
		assertThrows(WalletNotFoundException.class, () -> walletService.depositAndGetNewBalance(idk, walletId, amount));
	}

	@Test
	void withdrawAndGetNewBalance_returnsNewBalance() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("15.00");
		when(walletRepository.withdrawAndGetNewBalance(walletId, amount)).thenReturn(Optional.of(new BigDecimal("85.00")));
		var idk = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).build();
		// act
		var result = walletService.withdrawAndGetNewBalance(idk, walletId, amount);
		// assert
		assertEquals(new BigDecimal("85.00"), result);
	}

	@Test
	void withdrawAndGetNewBalance_throwsWhenWalletMissing() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("5.00");
		when(walletRepository.withdrawAndGetNewBalance(walletId, amount)).thenReturn(Optional.empty());
		when(walletRepository.existsById(walletId)).thenReturn(false);
		var idk = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).build();
		// act & assert
		assertThrows(WalletNotFoundException.class, () -> walletService.withdrawAndGetNewBalance(idk, walletId, amount));
	}

	@Test
	void withdrawAndGetNewBalance_throwsWhenInsufficientFunds() {
		// arrange
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("200.00");
		when(walletRepository.withdrawAndGetNewBalance(walletId, amount)).thenReturn(Optional.empty());
		when(walletRepository.existsById(walletId)).thenReturn(true);
		var idk = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).build();
		// act & assert
		var ex = assertThrows(InsufficientFundsException.class, () -> walletService.withdrawAndGetNewBalance(idk, walletId, amount));
		assertEquals(walletId, ex.getWalletId());
		assertEquals(amount, ex.getAttemptedAmount());
	}

	@Test
	void getBalanceAsOf_returnsBalance() {
		// arrange
		var walletId = UUID.randomUUID();
		var createdAt = OffsetDateTime.now().minusHours(2);
		var wallet = Wallet.builder()
			.walletId(walletId)
			.currentBalance(new BigDecimal("150.00"))
			.createdAt(createdAt)
			.updatedAt(createdAt.plusMinutes(5))
			.build();
		when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
		var at = OffsetDateTime.now().minusHours(1);
		when(ledgerService.getBalanceAsOf(walletId, at)).thenReturn(new BigDecimal("123.456"));
		// act
		var dto = walletService.getBalanceAsOf(walletId, at);
		// assert
		verify(walletValidator).validateAt(at);
		assertEquals(walletId, dto.getWalletId());
		assertEquals(new BigDecimal("123.46"), dto.getBalance());
		assertEquals(at, dto.getAsOf());
	}

	@Test
	void getBalanceAsOf_returnsZeroBeforeCreation() {
		// arrange
		var walletId = UUID.randomUUID();
		var createdAt = OffsetDateTime.now();
		var wallet = Wallet.builder()
			.walletId(walletId)
			.currentBalance(new BigDecimal("50.00"))
			.createdAt(createdAt)
			.updatedAt(createdAt)
			.build();
		when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
		var at = createdAt.minusDays(1);
		// act
		var dto = walletService.getBalanceAsOf(walletId, at);
		// assert
		verify(walletValidator).validateAt(at);
		assertEquals(new BigDecimal("0.00"), dto.getBalance());
	}

	@Test
	void listLedger_returnsPagedEntries() {
		// arrange
		var walletId = UUID.randomUUID();
		var createdAt = OffsetDateTime.now().minusDays(3);
		var wallet = Wallet.builder()
			.walletId(walletId)
			.currentBalance(new BigDecimal("20.00"))
			.createdAt(createdAt)
			.updatedAt(createdAt.plusHours(1))
			.build();
		when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
		var from = OffsetDateTime.now().minusDays(2);
		var to = OffsetDateTime.now().minusDays(1);
		var page = 0;
		var size = 2;
		var entry1 = LedgerEntry.builder()
			.ledgerId(UUID.randomUUID())
			.txId(UUID.randomUUID())
			.walletId(walletId)
			.amount(new BigDecimal("10.123"))
			.postingType(PostingType.DEPOSIT)
			.createdAt(from.plusHours(1))
			.currentBalance(new BigDecimal("10.123"))
			.build();
		var entry2 = LedgerEntry.builder()
			.ledgerId(UUID.randomUUID())
			.txId(UUID.randomUUID())
			.walletId(walletId)
			.amount(new BigDecimal("-5.500"))
			.postingType(PostingType.WITHDRAW)
			.createdAt(from.plusHours(2))
			.currentBalance(new BigDecimal("4.623"))
			.build();
		when(ledgerService.findPage(walletId, page, size, from, to)).thenReturn(java.util.List.of(entry1, entry2));
		when(ledgerService.count(walletId, from, to)).thenReturn(2L);
		// act
		var dto = walletService.listLedger(walletId, page, size, from, to);
		// assert
		verify(walletValidator).validateDateRange(from, to);
		assertEquals(2, dto.getEntries().size());
	}
}
