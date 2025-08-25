package com.bank.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ConcurrencyIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17-alpine")
		.withDatabaseName("wallet")
		.withUsername("wallet_user")
		.withPassword("wallet_password")
		.withEnv("POSTGRES_INITDB_ARGS", "--data-checksums")
		.withCommand("postgres", "-c", "deadlock_timeout=100ms", "-c", "log_lock_waits=on");

	@Autowired
	TestRestTemplate http;

	record WalletResp(String wallet_id, String walletId, String currentBalance, String current_balance) {
	}

	@Test
	@Timeout(60)
	void concurrentDeposits_sumMatches() {
		// arrange
		var walletId = createWalletRequest();
		var threads = 100;
		var perThread = 200;
		var amount = "1.00";

		// act
		runParallel(threads, perThread, () -> depositRequest(walletId, amount));

		// assert
		var expected = threads * perThread * Double.parseDouble(amount);
		var actual = Double.parseDouble(getBalanceRequest(walletId));
		assertThat(Math.abs(actual - expected)).isLessThan(0.0001);
	}

	@Test
	@Timeout(90)
	void crossTransfers_noDeadlocks_balancesConserved() {
		// arrange
		var a = createWalletRequest();
		var b = createWalletRequest();
		depositRequest(a, "1000.00");
		depositRequest(b, "1000.00");
		var threads = 50;
		var perThread = 100;
		var aExpectedBalance = new AtomicReference<>(BigDecimal.valueOf(1000.00D));
		var bExpectedBalance = new AtomicReference<>(BigDecimal.valueOf(1000.00D));

		// act
		runParallel(threads, perThread, () -> {
			if (ThreadLocalRandom.current().nextBoolean()) {
				transferRequest(a, b, "1.00");
				aExpectedBalance.updateAndGet(bal -> bal.subtract(BigDecimal.valueOf(1.00D)));
				bExpectedBalance.updateAndGet(bal -> bal.add(BigDecimal.valueOf(1.00D)));
			} else {
				transferRequest(b, a, "1.00");
				bExpectedBalance.updateAndGet(bal -> bal.subtract(BigDecimal.valueOf(1.00D)));
				aExpectedBalance.updateAndGet(bal -> bal.add(BigDecimal.valueOf(1.00D)));
			}
		});

		// assert
		var balanceA = Double.parseDouble(getBalanceRequest(a));
		var balanceB = Double.parseDouble(getBalanceRequest(b));
		var sum = balanceA + balanceB;
		assertThat(Math.abs(sum - 2000.00)).isLessThan(0.0001);
		assertThat(balanceA).isGreaterThanOrEqualTo(0.0);
		assertThat(balanceB).isGreaterThanOrEqualTo(0.0);
		assertThat(Math.abs(balanceA - aExpectedBalance.get().doubleValue())).isLessThan(0.0001);
		assertThat(Math.abs(balanceB - bExpectedBalance.get().doubleValue())).isLessThan(0.0001);
	}

	// Helpers

	String createWalletRequest() {
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_JSON);
		h.add("Idempotency-Key", UUID.randomUUID().toString());
		ResponseEntity<WalletResp> res = http.postForEntity(URI.create("/api/v1/wallets"), new HttpEntity<>("{}", h), WalletResp.class);
		assertThat(res.getStatusCode().value()).isEqualTo(201);
		return res.getBody().wallet_id() != null ? res.getBody().wallet_id() : res.getBody().walletId();
	}

	void depositRequest(String walletId, String amount) {
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_JSON);
		h.add("Idempotency-Key", UUID.randomUUID().toString());
		http.postForEntity(URI.create("/api/v1/wallets/" + walletId + "/deposit"),
			new HttpEntity<>("{\"amount\":\"" + amount + "\"}", h), String.class);
	}

	void transferRequest(String from, String to, String amount) {
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_JSON);
		h.add("Idempotency-Key", UUID.randomUUID().toString());
		http.postForEntity(URI.create("/api/v1/transfers"),
			new HttpEntity<>("{\"fromWalletId\":\"" + from + "\",\"toWalletId\":\"" + to + "\",\"amount\":\"" + amount + "\"}", h),
			String.class);
	}

	String getBalanceRequest(String walletId) {
		var res = http.getForEntity(URI.create("/api/v1/wallets/" + walletId), WalletResp.class);
		assertThat(res.getStatusCode().value()).isEqualTo(200);
		var body = res.getBody();
		return body.currentBalance() != null ? body.currentBalance() : body.current_balance();
	}

	void runParallel(int threads, int perThread, Runnable task) {
		try (var pool = Executors.newFixedThreadPool(threads)) {
			CompletableFuture.allOf(
				java.util.stream.IntStream.range(0, threads)
					.mapToObj(t -> CompletableFuture.runAsync(() -> {
						for (var i = 0; i < perThread; i++) task.run();
					}, pool))
					.toArray(CompletableFuture[]::new)
			).join();
		}
	}
}
