package com.bank.wallet.mapper;

import com.bank.wallet.dto.wallet.TransactionResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TransactionMapper {

    public TransactionResponseDto toResponseDto(UUID txId, UUID walletId, BigDecimal newBalance) {
        return TransactionResponseDto.builder()
            .transactionId(txId)
            .walletId(walletId)
            .newBalance(newBalance)
            .build();
    }
}
