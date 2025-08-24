package com.bank.wallet.controller.wallet;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.dto.wallet.WalletResponseDto;
import com.bank.wallet.dto.wallet.BalanceHistoryResponseDto;
import com.bank.wallet.service.TransactionService;
import com.bank.wallet.service.WalletService;
import com.bank.wallet.mapper.WalletMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/wallets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class WalletController implements WalletApi {

	private final WalletService walletService;
	private final TransactionService transactionService;
	private final WalletMapper walletMapper;

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WalletResponseDto createWallet() {
		return walletService.createWallet();
	}

	@Override
	@GetMapping("/{walletId}")
	public WalletResponseDto getWallet(@PathVariable UUID walletId) {
		return walletService.getWallet(walletId);
	}

	@Override
	@PostMapping("/{walletId}/deposit")
	public ResponseEntity<String> deposit(
		@PathVariable UUID walletId,
		@Valid @RequestBody TransactionRequestDto request,
		@RequestHeader("Idempotency-Key") UUID idempotencyKey
	) {
		return transactionService.deposit(walletId, request, idempotencyKey);
	}

	@Override
	@PostMapping("/{walletId}/withdraw")
	public ResponseEntity<String> withdraw(
		@PathVariable UUID walletId,
		@Valid @RequestBody TransactionRequestDto request,
		@RequestHeader("Idempotency-Key") UUID idempotencyKey
	) {
		return transactionService.withdraw(walletId, request, idempotencyKey);
	}

	@Override
	@GetMapping("/{walletId}/balance/history")
	public BalanceHistoryResponseDto getHistoricalBalance(@PathVariable UUID walletId, @RequestParam("at") OffsetDateTime at) {
		return walletService.getBalanceAsOf(walletId, at);
	}
}
