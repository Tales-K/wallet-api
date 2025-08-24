package com.bank.wallet.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class WalletValidator {
	public void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
		if (from.isAfter(to)) throw new IllegalArgumentException("from must be before or equal to to");
		if (from.plusYears(1).isBefore(to)) throw new IllegalArgumentException("Maximum allowed range is 1 year");
		if (to.isAfter(OffsetDateTime.now())) throw new IllegalArgumentException("to must be <= now");
	}

	public void validateAt(OffsetDateTime at) {
		if (at.isAfter(OffsetDateTime.now())) throw new IllegalArgumentException("at must be <= now");
	}
}
