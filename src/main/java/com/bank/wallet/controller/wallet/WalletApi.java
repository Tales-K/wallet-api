package com.bank.wallet.controller.wallet;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.dto.wallet.BalanceHistoryResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.UUID;

@Tag(name = "Wallet Management", description = "Operations related to wallet creation and management")
public interface WalletApi {

	@Operation(
		summary = "Create a new wallet",
		description = "Creates a new wallet for a user with optional initial balance"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Wallet created successfully"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping
	WalletResponseDto createWallet();

	@Operation(
		summary = "Get wallet by ID",
		description = "Retrieves wallet information by wallet ID"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Wallet found successfully"),
		@ApiResponse(responseCode = "404", description = "Wallet not found"),
		@ApiResponse(responseCode = "400", description = "Invalid wallet ID format")
	})
	@GetMapping("/{walletId}")
	WalletResponseDto getWallet(@PathVariable UUID walletId);

	@Operation(
		summary = "Deposit funds to wallet",
		description = "Adds funds to the specified wallet. Requires Idempotency-Key header for safe retries."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Deposit successful"),
		@ApiResponse(responseCode = "400", description = "Invalid request data or missing Idempotency-Key header"),
		@ApiResponse(responseCode = "404", description = "Wallet not found"),
		@ApiResponse(responseCode = "409", description = "Idempotency conflict or request in progress"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/{walletId}/deposit")
	ResponseEntity<String> deposit(
		@PathVariable UUID walletId,
		@Valid @RequestBody TransactionRequestDto request,
		@Parameter(description = "Idempotency key for safe retries", required = true)
		@RequestHeader("Idempotency-Key") UUID idempotencyKey
	);

	@Operation(
		summary = "Withdraw funds from wallet",
		description = "Removes funds from the specified wallet if sufficient balance exists. Requires Idempotency-Key header for safe retries."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Withdrawal successful"),
		@ApiResponse(responseCode = "400", description = "Invalid request data or missing Idempotency-Key header"),
		@ApiResponse(responseCode = "404", description = "Wallet not found"),
		@ApiResponse(responseCode = "409", description = "Insufficient funds, idempotency conflict, or request in progress"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/{walletId}/withdraw")
	ResponseEntity<String> withdraw(
		@PathVariable UUID walletId,
		@Valid @RequestBody TransactionRequestDto request,
		@Parameter(description = "Idempotency key for safe retries", required = true)
		@RequestHeader("Idempotency-Key") UUID idempotencyKey
	);

	@Operation(
		summary = "Get historical balance",
		description = "Retrieves the wallet balance as of the provided timestamp (inclusive)."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Historical balance computed"),
		@ApiResponse(responseCode = "400", description = "Invalid timestamp format"),
		@ApiResponse(responseCode = "404", description = "Wallet not found")
	})
	@GetMapping("/{walletId}/balance/history")
	BalanceHistoryResponseDto getHistoricalBalance(
		@PathVariable UUID walletId,
		@Parameter(description = "Timestamp in ISO-8601 format, e.g. 2025-01-01T00:00:00Z", required = true)
		@RequestParam("at") OffsetDateTime at
	);
}
