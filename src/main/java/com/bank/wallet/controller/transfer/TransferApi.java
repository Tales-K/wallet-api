package com.bank.wallet.controller.transfer;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Transfers", description = "Wallet to wallet transfer operations")
public interface TransferApi {

	@Operation(summary = "Create transfer", description = "Atomically moves funds between two wallets")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Transfer completed"),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "404", description = "Wallet not found"),
		@ApiResponse(responseCode = "409", description = "Conflict (idempotency or insufficient funds)"),
		@ApiResponse(responseCode = "422", description = "Semantic validation error"),
		@ApiResponse(responseCode = "500", description = "Internal error")
	})
	ResponseEntity<String> createTransfer(
		@Valid TransferRequestDto request,
		@Parameter(required = true) UUID idempotencyKey
	);
}
