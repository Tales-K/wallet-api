package com.bank.wallet.service;

import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.enums.PostingType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LedgerValidator {
	public void validate(LedgerEntry entry) {
		if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) == 0) {
			throw new IllegalArgumentException("Ledger amount cannot be zero");
		}
		if (entry.getPostingType() == null) {
			throw new IllegalArgumentException("Posting type is required");
		}

		var type = entry.getPostingType();
		var amount = entry.getAmount();

		var shouldBePositive = type == PostingType.DEPOSIT || type == PostingType.TRANSFER_CREDIT;
		if (shouldBePositive && amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be positive for " + type);
		}

		var shouldBeNegative = type == PostingType.WITHDRAW || type == PostingType.TRANSFER_DEBIT;
		if (shouldBeNegative && amount.compareTo(BigDecimal.ZERO) >= 0) {
			throw new IllegalArgumentException("Amount must be negative for " + type);
		}

		var isTransferPosting = type == PostingType.TRANSFER_DEBIT || type == PostingType.TRANSFER_CREDIT;
		if (isTransferPosting && entry.getTransferId() == null) {
			throw new IllegalArgumentException("Transfer ID is required for " + type);
		}

		var isDirectPosting = type == PostingType.DEPOSIT || type == PostingType.WITHDRAW;
		if (isDirectPosting && entry.getTransferId() != null) {
			throw new IllegalArgumentException("Transfer ID must be null for " + type);
		}
	}
}
