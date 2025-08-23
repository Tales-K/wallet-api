package com.bank.wallet.service;

import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.enums.PostingType;
import com.bank.wallet.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

	private final LedgerEntryRepository ledgerEntryRepository;
	private final LedgerValidator ledgerValidator;

	public void createDepositEntry(UUID txId, UUID walletId, BigDecimal amount) {
		log.debug("Creating deposit ledger entry: txId={}, walletId={}, amount={}", txId, walletId, amount);

		var ledgerEntry = LedgerEntry.builder()
			.txId(txId)
			.walletId(walletId)
			.amount(amount) // Positive for deposit
			.postingType(PostingType.DEPOSIT)
			.build();

		saveLedgeEntry(ledgerEntry);
		log.debug("Deposit ledger entry created successfully: {}", txId);
	}

	public void createWithdrawEntry(UUID txId, UUID walletId, BigDecimal amount) {
		log.debug("Creating withdraw ledger entry: txId={}, walletId={}, amount={}", txId, walletId, amount);

		var ledgerEntry = LedgerEntry.builder()
			.txId(txId)
			.walletId(walletId)
			.amount(amount.negate()) // Negative for withdrawal
			.postingType(PostingType.WITHDRAW)
			.build();

		saveLedgeEntry(ledgerEntry);
		log.debug("Withdraw ledger entry created successfully: {}", txId);
	}

	private void saveLedgeEntry(LedgerEntry ledgerEntry) {
		ledgerValidator.validate(ledgerEntry);
		ledgerEntryRepository.save(ledgerEntry);
	}

}
