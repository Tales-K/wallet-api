package com.bank.wallet.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDto {
	private UUID transferId;
	private UUID fromWalletId;
	private UUID toWalletId;
	private BigDecimal amount;
}
