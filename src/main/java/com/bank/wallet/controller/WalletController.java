package com.bank.wallet.controller;

import com.bank.wallet.config.WalletProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
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
public class WalletController implements WalletApi {

	private final DataSource dataSource;
	private final WalletProperties walletProperties;
	private final Environment environment;

	@Override
	public ResponseEntity<Map<String, Object>> getWalletStatus() {
		log.info("Wallet status requested at {}", LocalDateTime.now());

		Map<String, Object> status = Map.of(
			"service", "wallet-api",
			"status", "UP",
			"timestamp", LocalDateTime.now(),
			"version", walletProperties.getApp().getVersion()
		);

		return ResponseEntity.ok(status);
	}

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
			return connection.isValid(2); // 2 second timeout
		} catch (Exception e) {
			log.error("Database health check failed", e);
			return false;
		}
	}
}
