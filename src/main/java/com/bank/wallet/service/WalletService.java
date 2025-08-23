package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.WalletResponse;
import com.bank.wallet.entity.Wallet;
import com.bank.wallet.mapper.WalletMapper;
import com.bank.wallet.repository.WalletRepository;
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

	@Transactional
	public WalletResponse createWallet() {
		var now = OffsetDateTime.now();

		var wallet = Wallet.builder()
			.currentBalance(BigDecimal.ZERO)
			.createdAt(now)
			.updatedAt(now)
			.build();

		var savedWallet = walletRepository.save(wallet);
		return walletMapper.mapToResponse(savedWallet);
	}

	public WalletResponse getWallet(UUID walletId) {
		var wallet = walletRepository.findById(walletId)
			.orElseThrow(() -> new IllegalArgumentException("Wallet not found with ID: " + walletId));

		return walletMapper.mapToResponse(wallet);
	}

}
