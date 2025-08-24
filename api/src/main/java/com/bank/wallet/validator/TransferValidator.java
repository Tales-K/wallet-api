package com.bank.wallet.validator;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.exception.SemanticValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferValidator {

	public void validate(TransferRequestDto requestDto, IdempotencyKey idempotencyKey) {
		if (requestDto.getFromWalletId() == null || requestDto.getToWalletId() == null)
			throw new SemanticValidationException("Wallet ids required", idempotencyKey);
		if (requestDto.getFromWalletId().equals(requestDto.getToWalletId()))
			throw new SemanticValidationException("from_wallet_id and to_wallet_id must differ", idempotencyKey);
		if (requestDto.getAmount() == null || requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new SemanticValidationException("Amount must be > 0", idempotencyKey);
	}

}

