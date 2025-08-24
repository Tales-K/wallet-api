package com.bank.wallet.service;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.entity.IdempotencyKey;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.exception.SemanticValidationException;
import com.bank.wallet.validator.TransferValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TransferValidatorTest {

	private final TransferValidator validator = new TransferValidator();

	@Test
	void validate_withoutWalletIds() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var req = TransferRequestDto.builder().fromWalletId(null).toWalletId(null).amount(new BigDecimal("10.00")).build();
		// act & assert
		assertThrows(SemanticValidationException.class, () -> validator.validate(req, key));
	}

	@Test
	void validate_sameWallet() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var id = UUID.randomUUID();
		var req = TransferRequestDto.builder().fromWalletId(id).toWalletId(id).amount(new BigDecimal("5.00")).build();
		// act & assert
		assertThrows(SemanticValidationException.class, () -> validator.validate(req, key));
	}

	@Test
	void validate_noAmount() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var req = TransferRequestDto.builder().fromWalletId(UUID.randomUUID()).toWalletId(UUID.randomUUID()).amount(null).build();
		// act & assert
		assertThrows(SemanticValidationException.class, () -> validator.validate(req, key));
	}

	@Test
	void validate_amountLessThanZero() {
		// arrange
		var key = IdempotencyKey.builder().idempotencyKey(UUID.randomUUID()).refId(UUID.randomUUID()).status(IdempotencyStatus.IN_PROGRESS).build();
		var req = TransferRequestDto.builder().fromWalletId(UUID.randomUUID()).toWalletId(UUID.randomUUID()).amount(new BigDecimal("-1.00")).build();
		// act & assert
		assertThrows(SemanticValidationException.class, () -> validator.validate(req, key));
	}
}
