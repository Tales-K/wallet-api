package com.bank.wallet.mapper;

import com.bank.wallet.dto.wallet.WalletResponse;
import com.bank.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

	public WalletResponse mapToResponse(Wallet wallet) {
		return WalletResponse.builder()
			.walletId(wallet.getWalletId())
			.currentBalance(wallet.getCurrentBalance())
			.createdAt(wallet.getCreatedAt())
			.updatedAt(wallet.getUpdatedAt())
			.build();
	}

}
