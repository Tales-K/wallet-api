package com.bank.wallet.service;

import com.bank.wallet.entity.LedgerEntry;
import com.bank.wallet.entity.enums.PostingType;
import com.bank.wallet.mapper.LedgerMapper;
import com.bank.wallet.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerValidator ledgerValidator;
    private final LedgerMapper ledgerMapper;

    private void insert(UUID txId, UUID walletId, BigDecimal amount, PostingType type, BigDecimal currentBalance) {
        var entry = ledgerMapper.create(txId, walletId, amount, type, currentBalance);
        ledgerValidator.validate(entry);
        var rows = ledgerEntryRepository.insertGeneric(txId, walletId, amount, type.name().toLowerCase(), currentBalance);
        if (rows != 1) {
            log.error("Ledger insert failed action={} txId={} walletId={} rows={}", type, txId, walletId, rows);
            throw new IllegalStateException("Ledger insertion failed: " + type);
        }
    }

    public void createDepositEntry(UUID txId, UUID walletId, BigDecimal amount, BigDecimal currentBalance) {
        insert(txId, walletId, amount, PostingType.DEPOSIT, currentBalance);
    }

    public void createWithdrawEntry(UUID txId, UUID walletId, BigDecimal amount, BigDecimal currentBalance) {
        insert(txId, walletId, amount.negate(), PostingType.WITHDRAW, currentBalance);
    }

    public void createTransferDebitEntry(UUID txId, UUID fromWalletId, BigDecimal amount, BigDecimal currentBalance) {
        insert(txId, fromWalletId, amount.negate(), PostingType.TRANSFER_DEBIT, currentBalance);
    }

    public void createTransferCreditEntry(UUID txId, UUID toWalletId, BigDecimal amount, BigDecimal currentBalance) {
        insert(txId, toWalletId, amount, PostingType.TRANSFER_CREDIT, currentBalance);
    }

    public BigDecimal getBalanceAsOf(UUID walletId, OffsetDateTime at) {
        return ledgerEntryRepository.findBalanceAsOf(walletId, at).orElse(BigDecimal.ZERO);
    }

    public List<LedgerEntry> findPage(UUID walletId, int page, int size, OffsetDateTime from, OffsetDateTime to) {
        var offset = page * size;
        return ledgerEntryRepository.findPage(walletId, from, to, size, offset);
    }

    public long count(UUID walletId, OffsetDateTime from, OffsetDateTime to) {
        return ledgerEntryRepository.countAll(walletId, from, to);
    }
}
