package com.bank.wallet.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDto implements Serializable {
	private UUID transactionId;
	private UUID fromWalletId;
	private UUID toWalletId;
	private BigDecimal amount;
}
