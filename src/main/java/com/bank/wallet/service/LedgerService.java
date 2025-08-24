package com.bank.wallet.service;

import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.enums.PostingType;
import com.bank.wallet.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerValidator ledgerValidator;

    public void createDepositEntry(UUID txId, UUID walletId, BigDecimal amount) {
        log.debug("Creating deposit ledger entry: txId={}, walletId={}, amount={}", txId, walletId, amount);
        var entry = LedgerEntry.builder()
            .txId(txId)
            .walletId(walletId)
            .amount(amount)
            .postingType(PostingType.DEPOSIT)
            .build();
        ledgerValidator.validate(entry);
        ledgerEntryRepository.insertDeposit(txId, walletId, amount);
        log.debug("Deposit ledger entry created successfully: {}", txId);
    }

    public void createWithdrawEntry(UUID txId, UUID walletId, BigDecimal amount) {
        log.debug("Creating withdraw ledger entry: txId={}, walletId={}, amount={}", txId, walletId, amount);
        var negative = amount.negate();
        var entry = LedgerEntry.builder()
            .txId(txId)
            .walletId(walletId)
            .amount(negative)
            .postingType(PostingType.WITHDRAW)
            .build();
        ledgerValidator.validate(entry);
        ledgerEntryRepository.insertWithdraw(txId, walletId, negative);
        log.debug("Withdraw ledger entry created successfully: {}", txId);
    }
}
