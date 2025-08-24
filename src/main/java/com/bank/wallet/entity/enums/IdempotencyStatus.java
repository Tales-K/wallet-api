package com.bank.wallet.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IdempotencyStatus {
	IN_PROGRESS("in_progress"),
	SUCCEEDED("succeeded"),
	FAILED("failed");

	private final String value;
}
