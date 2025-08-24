package com.bank.wallet.mapper;

import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.enums.PostingType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class LedgerMapper {
	public LedgerEntry create(UUID txId, UUID walletId, BigDecimal amount, PostingType type, BigDecimal currentBalance) {
		return LedgerEntry.builder()
			.txId(txId)
			.walletId(walletId)
			.amount(amount)
			.postingType(type)
			.currentBalance(currentBalance)
			.build();
	}
}
