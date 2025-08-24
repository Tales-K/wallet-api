package com.bank.wallet.service;

import com.bank.wallet.entity.enums.PostingType;
import com.bank.wallet.mapper.LedgerMapper;
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
    private final LedgerMapper ledgerMapper;

    private void insert(UUID txId, UUID walletId, BigDecimal amount, PostingType type) {
        var entry = ledgerMapper.create(txId, walletId, amount, type);
        ledgerValidator.validate(entry);
        var rows = ledgerEntryRepository.insertGeneric(txId, walletId, amount, type.name().toLowerCase());
        if (rows != 1) {
            log.error("Ledger insert failed action={} txId={} walletId={} rows={}", type, txId, walletId, rows);
            throw new IllegalStateException("Ledger insertion failed: " + type);
        }
    }

    public void createDepositEntry(UUID txId, UUID walletId, BigDecimal amount) {
        insert(txId, walletId, amount, PostingType.DEPOSIT);
    }

    public void createWithdrawEntry(UUID txId, UUID walletId, BigDecimal amount) {
        insert(txId, walletId, amount.negate(), PostingType.WITHDRAW);
    }

    public void createTransferDebitEntry(UUID txId, UUID fromWalletId, BigDecimal amount) {
        insert(txId, fromWalletId, amount.negate(), PostingType.TRANSFER_DEBIT);
    }

    public void createTransferCreditEntry(UUID txId, UUID toWalletId, BigDecimal amount) {
        insert(txId, toWalletId, amount, PostingType.TRANSFER_CREDIT);
    }
}
