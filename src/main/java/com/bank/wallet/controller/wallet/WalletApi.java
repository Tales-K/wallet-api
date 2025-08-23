package com.bank.wallet.controller.wallet;

import com.bank.wallet.dto.wallet.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@Tag(name = "Wallet Management", description = "Operations related to wallet creation and management")
public interface WalletApi {

	@Operation(
		summary = "Create a new wallet",
		description = "Creates a new wallet for a user with optional initial balance"
	)
	@PostMapping("/create")
	WalletResponse createWallet();

	@Operation(
		summary = "Get wallet by ID",
		description = "Retrieves wallet information by wallet ID"
	)
	@GetMapping("/{walletId}")
	WalletResponse getWallet(@PathVariable UUID walletId);
}
