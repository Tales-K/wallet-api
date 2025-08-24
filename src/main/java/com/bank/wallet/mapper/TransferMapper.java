package com.bank.wallet.mapper;

import com.bank.wallet.dto.transfer.TransferResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TransferMapper {
	public TransferResponseDto toResponse(UUID transferId, UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
		return TransferResponseDto.builder()
			.transferId(transferId)
			.fromWalletId(fromWalletId)
			.toWalletId(toWalletId)
			.amount(amount)
			.build();
	}
}

