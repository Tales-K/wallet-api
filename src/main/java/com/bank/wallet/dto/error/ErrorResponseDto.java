package com.bank.wallet.dto.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDto implements Serializable {

	private String code;

	private String message;

	private OffsetDateTime timestamp;

	private UUID transactionIdentifier;

	private Map<String, Object> details;
}
