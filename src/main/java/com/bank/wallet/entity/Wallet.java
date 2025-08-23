package com.bank.wallet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

	@Id
	@Column("wallet_id")
	private UUID walletId;

	@Column("current_balance")
	@Builder.Default
	private BigDecimal currentBalance = BigDecimal.ZERO;

	@CreatedDate
	@Column("created_at")
	private OffsetDateTime createdAt;

	@LastModifiedDate
	@Column("updated_at")
	private OffsetDateTime updatedAt;
}
