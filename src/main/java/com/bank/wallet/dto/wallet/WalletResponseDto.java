package com.bank.wallet.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponseDto {
	private UUID walletId;
	private BigDecimal currentBalance;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}
