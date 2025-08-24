package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.exception.SemanticValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferValidator {
	public void validate(TransferRequestDto requestDto) {
		if (requestDto.getFromWalletId() == null || requestDto.getToWalletId() == null)
			throw new SemanticValidationException("Wallet ids required");
		if (requestDto.getFromWalletId().equals(requestDto.getToWalletId()))
			throw new SemanticValidationException("from_wallet_id and to_wallet_id must differ");
		if (requestDto.getAmount() == null || requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new SemanticValidationException("Amount must be > 0");
	}
}

