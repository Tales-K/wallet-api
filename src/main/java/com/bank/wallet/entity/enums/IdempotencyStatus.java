package com.bank.wallet.entity.enums;

public enum IdempotencyStatus {
	IN_PROGRESS,
	COMPLETED // transactions failed by business reasons like 'insufficient funds' still count as completed
	// failed registers won't be saved/cached because they result from an unexpected application error.
}
