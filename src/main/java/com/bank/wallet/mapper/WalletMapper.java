package com.bank.wallet.mapper;

import com.bank.wallet.dto.wallet.BalanceHistoryResponseDto;
import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class WalletMapper {

	public WalletResponseDto mapToResponse(Wallet wallet) {
		return WalletResponseDto.builder()
			.walletId(wallet.getWalletId())
			.currentBalance(wallet.getCurrentBalance().setScale(2, RoundingMode.HALF_UP))
			.createdAt(wallet.getCreatedAt())
			.updatedAt(wallet.getUpdatedAt())
			.build();
	}

	public BalanceHistoryResponseDto mapToBalanceHistory(UUID walletId, BigDecimal balance, OffsetDateTime asOf) {
		return BalanceHistoryResponseDto.builder()
			.walletId(walletId)
			.balance(balance.setScale(2, RoundingMode.HALF_UP))
			.asOf(asOf)
			.build();
	}
}
