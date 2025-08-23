package com.bank.wallet.entity;

import com.bank.wallet.entity.enums.PostingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "ledger_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

	@Id
	@Column("ledger_id")
	private UUID ledgerId;

	@Column("tx_id")
	private UUID txId;

	@Column("wallet_id")
	private UUID walletId;

	@Column("amount")
	private BigDecimal amount;

	@Column("posting_type")
	private PostingType postingType;

	@Column("transfer_id")
	private UUID transferId;

	@CreatedDate
	@Column("created_at")
	private OffsetDateTime createdAt;

	public void validateLedgerEntry() {
		// Amount cannot be zero
		if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
			throw new IllegalArgumentException("Ledger amount cannot be zero");
		}

		if (postingType == null) {
			throw new IllegalArgumentException("Posting type is required");
		}

		// Validate amount sign based on posting type
		boolean shouldBePositive = postingType == PostingType.DEPOSIT || postingType == PostingType.TRANSFER_CREDIT;
		boolean shouldBeNegative = postingType == PostingType.WITHDRAW || postingType == PostingType.TRANSFER_DEBIT;

		if (shouldBePositive && amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be positive for " + postingType);
		}
		if (shouldBeNegative && amount.compareTo(BigDecimal.ZERO) >= 0) {
			throw new IllegalArgumentException("Amount must be negative for " + postingType);
		}


		// Validate transfer_id presence based on posting type
		boolean isTransferPosting = postingType == PostingType.TRANSFER_DEBIT || postingType == PostingType.TRANSFER_CREDIT;
		boolean isDirectPosting = postingType == PostingType.DEPOSIT || postingType == PostingType.WITHDRAW;

		if (isTransferPosting && transferId == null) {
			throw new IllegalArgumentException("Transfer ID is required for " + postingType);
		}
		if (isDirectPosting && transferId != null) {
			throw new IllegalArgumentException("Transfer ID must be null for " + postingType);
		}
	}

	public boolean isDebit() {
		return postingType == PostingType.WITHDRAW || postingType == PostingType.TRANSFER_DEBIT;
	}

	public boolean isCredit() {
		return postingType == PostingType.DEPOSIT || postingType == PostingType.TRANSFER_CREDIT;
	}
}
