package com.bank.wallet.service;

import com.bank.wallet.entity.enums.PostingType;
import com.bank.wallet.mapper.LedgerMapper;
import com.bank.wallet.repository.LedgerEntryRepository;
import com.bank.wallet.validator.LedgerValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

	@Mock
	private LedgerEntryRepository ledgerEntryRepository;
	@Mock
	private LedgerValidator ledgerValidator;
	@Spy
	private LedgerMapper ledgerMapper = new LedgerMapper();
	@InjectMocks
	private LedgerService ledgerService;

	@Test
	void createDepositEntry_insertsRow() {
		// arrange
		var txId = UUID.randomUUID();
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("25.00");
		var newBal = new BigDecimal("125.00");
		when(ledgerEntryRepository.insertGeneric(txId, walletId, amount, PostingType.DEPOSIT.name().toLowerCase(), newBal)).thenReturn(1);
		// act
		ledgerService.createDepositEntry(txId, walletId, amount, newBal);
		// assert
		verify(ledgerValidator).validate(any());
	}

	@Test
	void createDepositEntry_throwsWhenNoRowsInserted() {
		// arrange
		var txId = UUID.randomUUID();
		var walletId = UUID.randomUUID();
		var amount = new BigDecimal("10.00");
		var newBal = new BigDecimal("60.00");
		when(ledgerEntryRepository.insertGeneric(txId, walletId, amount, "deposit", newBal)).thenReturn(0);
		// act & assert
		assertThrows(IllegalStateException.class, () -> ledgerService.createDepositEntry(txId, walletId, amount, newBal));
	}
}
