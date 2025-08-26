package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.BalanceHistoryResponseDto;
import com.bank.wallet.dto.wallet.LedgerPageResponseDto;
import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.Wallet;
import com.bank.wallet.exception.InsufficientFundsException;
import com.bank.wallet.exception.WalletNotFoundException;
import com.bank.wallet.mapper.WalletMapper;
import com.bank.wallet.repository.WalletRepository;
import com.bank.wallet.validator.WalletValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

	private final WalletRepository walletRepository;
	private final WalletMapper walletMapper;
	private final LedgerService ledgerService;
	private final WalletValidator walletValidator;

	@Transactional
	public WalletResponseDto createWallet() {
		log.info("Creating wallet");
		var now = OffsetDateTime.now();
		var wallet = Wallet.builder()
			.currentBalance(BigDecimal.ZERO)
			.createdAt(now)
			.updatedAt(now)
			.build();
		var savedWallet = walletRepository.save(wallet);
		return walletMapper.mapToResponse(savedWallet);
	}

	public WalletResponseDto getWallet(UUID walletId) {
		var wallet = this.findById(walletId);
		return walletMapper.mapToResponse(wallet);
	}

	@Transactional
	public BigDecimal depositAndGetNewBalance(IdempotencyKey idempotencyKey, UUID walletId, BigDecimal amount) {
		var newBalance = walletRepository.depositAndGetNewBalance(walletId, amount);
		if (newBalance.isEmpty()) throw new WalletNotFoundException("Wallet not found: " + walletId, idempotencyKey);
		return newBalance.get();
	}

	@Transactional
	public BigDecimal withdrawAndGetNewBalance(IdempotencyKey idempotencyKey, UUID walletId, BigDecimal amount) {
		var newBalance = walletRepository.withdrawAndGetNewBalance(walletId, amount);
		if (newBalance.isEmpty()) {
			if (!walletRepository.existsById(walletId))
				throw new WalletNotFoundException("Wallet not found: " + walletId, idempotencyKey);
			throw new InsufficientFundsException("Insufficient funds for withdrawal", walletId, amount, idempotencyKey);
		}
		return newBalance.get();
	}

	public BalanceHistoryResponseDto getBalanceAsOf(UUID walletId, OffsetDateTime at) {
		walletValidator.validateAt(at);
		var wallet = this.findById(walletId);

		var balance = at.isBefore(wallet.getCreatedAt())
			? BigDecimal.ZERO
			: ledgerService.getBalanceAsOf(walletId, at);

		return walletMapper.mapToBalanceHistory(walletId, balance, at);
	}

	public LedgerPageResponseDto listLedger(UUID walletId, int page, int size, OffsetDateTime from, OffsetDateTime to) {
		walletValidator.validateDateRange(from, to);
		var wallet = this.findById(walletId);
		var entries = ledgerService.findPage(wallet.getWalletId(), page, size, from, to);
		var total = ledgerService.count(wallet.getWalletId(), from, to);
		var dtoEntries = walletMapper.mapLedgerEntries(entries);
		return walletMapper.mapLedgerPage(page, size, total, dtoEntries);
	}

	private Wallet findById(UUID walletId) {
		return walletRepository.findById(walletId)
			.orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
	}
}
