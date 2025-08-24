package com.bank.wallet.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletValidatorTest {
	private final WalletValidator validator = new WalletValidator();

	@Test
	void validateDateRange_allowsValidPastRange() {
		var now = OffsetDateTime.now();
		var from = now.minusDays(10);
		var to = now.minusDays(1);
		assertDoesNotThrow(() -> validator.validateDateRange(from, to));
	}

	@Test
	void validateDateRange_rejectsFutureTo() {
		var now = OffsetDateTime.now();
		var from = now.minusDays(2);
		var to = now.plusMinutes(1);
		assertThrows(IllegalArgumentException.class, () -> validator.validateDateRange(from, to));
	}

	@Test
	void validateAt_allowsPastOrPresent() {
		var at = OffsetDateTime.now().minusSeconds(1);
		assertDoesNotThrow(() -> validator.validateAt(at));
	}

	@Test
	void validateAt_rejectsFuture() {
		var at = OffsetDateTime.now().plusSeconds(1);
		assertThrows(IllegalArgumentException.class, () -> validator.validateAt(at));
	}
}

