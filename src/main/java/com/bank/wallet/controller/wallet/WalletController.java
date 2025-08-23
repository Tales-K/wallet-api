package com.bank.wallet.controller.wallet;

import com.bank.wallet.dto.wallet.TransactionRequestDto;
import com.bank.wallet.dto.wallet.WalletResponse;
import com.bank.wallet.service.TransactionService;
import com.bank.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController implements WalletApi {

	private final WalletService walletService;
	private final TransactionService transactionService;

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WalletResponse createWallet() {
		return walletService.createWallet();
	}

	@Override
	@GetMapping("/{walletId}")
	public WalletResponse getWallet(@PathVariable UUID walletId) {
		return walletService.getWallet(walletId);
	}

	@Override
	@PostMapping("/{walletId}/deposit")
	public ResponseEntity<String> deposit(@PathVariable UUID walletId, @Valid @RequestBody TransactionRequestDto request, @RequestHeader("Idempotency-Key") String idempotencyKey) {
		return transactionService.deposit(walletId, request, idempotencyKey);
	}

	@Override
	@PostMapping("/{walletId}/withdraw")
	public ResponseEntity<String> withdraw(@PathVariable UUID walletId, @Valid @RequestBody TransactionRequestDto request, @RequestHeader("Idempotency-Key") String idempotencyKey) {
		return transactionService.withdraw(walletId, request, idempotencyKey);
	}
}
