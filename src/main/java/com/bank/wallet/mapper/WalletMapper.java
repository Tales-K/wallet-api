package com.bank.wallet.mapper;

import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

	public WalletResponseDto mapToResponse(Wallet wallet) {
		return WalletResponseDto.builder()
			.walletId(wallet.getWalletId())
			.currentBalance(wallet.getCurrentBalance())
			.createdAt(wallet.getCreatedAt())
			.updatedAt(wallet.getUpdatedAt())
			.build();
	}

}
