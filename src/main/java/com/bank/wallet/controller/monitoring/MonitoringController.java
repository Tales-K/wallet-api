package com.bank.wallet.controller.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@Slf4j
@RequiredArgsConstructor
public class MonitoringController implements MonitoringApi {

	private final DataSource dataSource;

	@Override
	public ResponseEntity<Map<String, Object>> health() {
		log.debug("Health check requested");

		boolean isDatabaseHealthy = checkDatabaseHealth();
		String overallStatus = isDatabaseHealthy ? "UP" : "DOWN";

		Map<String, Object> healthStatus = Map.of(
			"status", overallStatus,
			"timestamp", LocalDateTime.now(),
			"checks", Map.of(
				"database", isDatabaseHealthy ? "UP" : "DOWN",
				"application", "UP"
			),
			"details", Map.of(
				"database", isDatabaseHealthy ? "Database connection is healthy" : "Database connection failed",
				"uptime", "Service is running"
			)
		);

		return isDatabaseHealthy ?
			ResponseEntity.ok(healthStatus) :
			ResponseEntity.status(503).body(healthStatus);
	}

	private boolean checkDatabaseHealth() {
		try (Connection connection = dataSource.getConnection()) {
			return connection.isValid(1); // timeout limit in seconds
		} catch (Exception e) {
			log.error("Database health check failed", e);
			return false;
		}
	}
}
