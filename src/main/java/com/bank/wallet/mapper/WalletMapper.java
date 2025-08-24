package com.bank.wallet.mapper;

import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

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

}
