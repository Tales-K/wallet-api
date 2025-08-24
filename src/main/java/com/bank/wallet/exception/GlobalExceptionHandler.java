package com.bank.wallet.exception;

import com.bank.wallet.dto.error.ErrorResponseDto;
import com.bank.wallet.entity.enums.IdempotencyStatus;
import com.bank.wallet.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final IdempotencyService idempotencyService;

	/* cached idempotent exceptions */
	@ExceptionHandler(WalletNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleWalletNotFound(WalletNotFoundException ex, HttpServletRequest request) {
		log.warn("Wallet not found: {}", ex.getMessage());
		return handleIdempotencyCachedError(ex, HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", null);
	}

	@ExceptionHandler(InsufficientFundsException.class)
	public ResponseEntity<ErrorResponseDto> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
		log.warn("Insufficient funds: {}", ex.getMessage());
		var details = Map.<String, Object>of(
			"walletId", ex.getWalletId().toString(),
			"attemptedAmount", ex.getAttemptedAmount().toString()
		);
		return handleIdempotencyCachedError(ex, HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", details);
	}

	@ExceptionHandler(SemanticValidationException.class)
	public ResponseEntity<ErrorResponseDto> handleSemantic(SemanticValidationException ex, HttpServletRequest request) {
		log.warn("Semantic validation error: {}", ex.getMessage());
		return handleIdempotencyCachedError(ex, HttpStatus.UNPROCESSABLE_ENTITY, "SEMANTIC_ERROR", null);
	}

	/* non-cached idempotency exceptions */
	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponseDto> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
		log.warn("Idempotency conflict: {}", ex.getMessage());
		var body = ErrorResponseDto.builder()
			.code(ex.getCode())
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(IdempotencyInProgressException.class)
	public ResponseEntity<ErrorResponseDto> handleIdempotencyInProgress(IdempotencyInProgressException ex, HttpServletRequest request) {
		log.warn("Idempotency in progress: {}", ex.getMessage());
		var body = ErrorResponseDto.builder()
			.code("IDEMPOTENCY_IN_PROGRESS")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	/* business exceptions */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		log.warn("Invalid argument: {}", ex.getMessage());
		var body = ErrorResponseDto.builder()
			.code("INVALID_REQUEST")
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.warn("Validation error: {}", ex.getMessage());
		var message = new StringBuilder("Validation failed: ");
		ex.getBindingResult().getFieldErrors().forEach(err -> message.append(err.getField()).append(" ").append(err.getDefaultMessage()).append("; "));
		var body = ErrorResponseDto.builder()
			.code("VALIDATION_ERROR")
			.message(message.toString())
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		log.warn("Type mismatch: {}", ex.getMessage());
		var requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown";
		var details = Map.<String, Object>of(
			"parameter", ex.getName(),
			"receivedValue", String.valueOf(ex.getValue()),
			"expectedType", requiredType
		);
		var message = ex.getMessage();
		var body = ErrorResponseDto.builder()
			.code("INVALID_FORMAT")
			.message(message)
			.timestamp(OffsetDateTime.now())
			.details(details)
			.build();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error", ex);
		var body = ErrorResponseDto.builder()
			.code("INTERNAL_ERROR")
			.message("An unexpected error occurred")
			.timestamp(OffsetDateTime.now())
			.build();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	private ResponseEntity<ErrorResponseDto> handleIdempotencyCachedError(TransactionRuntimeException ex, HttpStatus status, String code, Map<String, Object> details) {
		var refId = ex.getIdempotencyKey() != null ? ex.getIdempotencyKey().getRefId() : null;
		var body = ErrorResponseDto.builder()
			.code(code)
			.message(ex.getMessage())
			.timestamp(OffsetDateTime.now())
			.transactionIdentifier(refId)
			.details(details)
			.build();

		if (ex.getIdempotencyKey() != null)
			idempotencyService.markCompleted(ex.getIdempotencyKey(), status.value(), body, IdempotencyStatus.FAILED);

		return ResponseEntity.status(status).body(body);
	}

}
