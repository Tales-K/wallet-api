package com.bank.wallet.service;

import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.entity.Wallet;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.exception.InsufficientFundsException;
import com.bank.wallet.exception.WalletNotFoundException;
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
    public WalletResponseDto createWallet() {
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
        var wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
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
            if (!walletRepository.existsById(walletId)) throw new WalletNotFoundException("Wallet not found: " + walletId, idempotencyKey);
            throw new InsufficientFundsException("Insufficient funds for withdrawal", walletId, amount, idempotencyKey);
        }
        return newBalance.get();
    }
}
