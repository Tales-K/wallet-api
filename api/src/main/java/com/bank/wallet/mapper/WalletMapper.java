package com.bank.wallet.mapper;

import com.bank.wallet.dto.wallet.BalanceHistoryResponseDto;
import com.bank.wallet.dto.wallet.LedgerEntryDto;
import com.bank.wallet.dto.wallet.LedgerPageResponseDto;
import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

	public List<LedgerEntryDto> mapLedgerEntries(List<LedgerEntry> entries) {
		return entries.stream().map(e -> LedgerEntryDto.builder()
			.txId(e.getTxId())
			.amount(e.getAmount().setScale(2, RoundingMode.HALF_UP))
			.postingType(e.getPostingType().name().toLowerCase())
			.currentBalance(e.getCurrentBalance().setScale(2, RoundingMode.HALF_UP))
			.createdAt(e.getCreatedAt())
			.build()).collect(Collectors.toList());
	}

	public LedgerPageResponseDto mapLedgerPage(int page, int size, long total, List<LedgerEntryDto> entries) {
		return LedgerPageResponseDto.builder()
			.page(page)
			.size(size)
			.total(total)
			.entries(entries)
			.build();
	}
}
