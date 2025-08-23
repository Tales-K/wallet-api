package com.bank.wallet.exception;

import com.bank.wallet.dto.error.ErrorResponseDto;
import com.bank.wallet.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final IdempotencyService idempotencyService;

	private void cacheFailed(HttpServletRequest request, HttpStatus status, ErrorResponseDto body) {
		var key = request.getHeader("Idempotency-Key");
		if (key == null || key.isBlank()) return;
		idempotencyService.markCompleted(key, status.value(), body);
	}

	@ExceptionHandler(WalletNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleWalletNotFound(WalletNotFoundException ex, HttpServletRequest request) {
		log.warn("Wallet not found: {}", ex.getMessage());
		var error = ErrorResponseDto.builder()
			.code("WALLET_NOT_FOUND")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		cacheFailed(request, HttpStatus.NOT_FOUND, error);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(InsufficientFundsException.class)
	public ResponseEntity<ErrorResponseDto> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
		log.warn("Insufficient funds: {}", ex.getMessage());
		var error = ErrorResponseDto.builder()
			.code("INSUFFICIENT_FUNDS")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.details(Map.of(
				"walletId", ex.getWalletId().toString(),
				"attemptedAmount", ex.getAttemptedAmount().toString()
			))
			.build();
		cacheFailed(request, HttpStatus.CONFLICT, error);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponseDto> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
		log.warn("Idempotency conflict: {}", ex.getMessage());
		var error = ErrorResponseDto.builder()
			.code(ex.getCode())
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(IdempotencyInProgressException.class)
	public ResponseEntity<ErrorResponseDto> handleIdempotencyInProgress(IdempotencyInProgressException ex, HttpServletRequest request) {
		log.warn("Idempotency in progress: {}", ex.getMessage());
		var error = ErrorResponseDto.builder()
			.code("IDEMPOTENCY_IN_PROGRESS")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		log.warn("Invalid argument: {}", ex.getMessage());
		var error = ErrorResponseDto.builder()
			.code("INVALID_REQUEST")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.warn("Validation error: {}", ex.getMessage());
		var message = new StringBuilder("Validation failed: ");
		ex.getBindingResult().getFieldErrors().forEach(error ->
			message.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; ")
		);
		var error = ErrorResponseDto.builder()
			.code("VALIDATION_ERROR")
			.message(message.toString())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(MissingIdempotencyKeyException.class)
	public ResponseEntity<ErrorResponseDto> handleMissingIdempotencyKey(MissingIdempotencyKeyException ex) {
		log.warn("Missing idempotency key: {}", ex.getMessage());
		var error = ErrorResponseDto.builder()
			.code("MISSING_IDEMPOTENCY_KEY")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error", ex);
		var error = ErrorResponseDto.builder()
			.code("INTERNAL_ERROR")
			.message("An unexpected error occurred")
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	public static class MissingIdempotencyKeyException extends RuntimeException {
		public MissingIdempotencyKeyException(String message) {
			super(message);
		}
	}
}
