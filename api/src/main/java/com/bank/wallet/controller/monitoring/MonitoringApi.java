package com.bank.wallet.controller.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.bank.wallet.dto.error.ErrorResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Tag(name = "Monitoring", description = "Monitoring")
public interface MonitoringApi {

	@Operation(
		summary = "Health check",
		description = "Comprehensive health check endpoint including database connectivity"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Service is healthy"),
		@ApiResponse(responseCode = "503", description = "Service is unhealthy",
			content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
	})
	@GetMapping("/health")
	ResponseEntity<Map<String, Object>> health();

}
