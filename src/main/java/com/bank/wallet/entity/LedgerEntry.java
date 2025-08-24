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

	@CreatedDate
	@Column("created_at")
	private OffsetDateTime createdAt;

}
