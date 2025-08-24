package com.bank.wallet.dto.transfer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
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
public class TransferRequestDto {
	@NotNull
	private UUID fromWalletId;
	@NotNull
	private UUID toWalletId;
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
	@DecimalMax(value = "9999999999999999.99", message = "Amount exceeds maximum allowed")
	@Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
	private BigDecimal amount;
}
