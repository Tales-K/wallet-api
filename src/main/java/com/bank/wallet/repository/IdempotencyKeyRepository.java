package com.bank.wallet.repository;

import com.bank.wallet.entity.IdempotencyKey;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends CrudRepository<IdempotencyKey, String> {

	@Query("SELECT * FROM idempotency_keys WHERE idempotency_key = :key")
	Optional<IdempotencyKey> findByIdempotencyKey(@Param("key") String key);

	@Modifying
	@Query("""
		INSERT INTO idempotency_keys (idempotency_key, method, path, request_hash, status, response_status, response_body, first_seen_at, last_seen_at)
		VALUES (:key, :method, :path, :requestHash, 'in_progress', NULL, NULL, now(), now())
		""")
	void insertNew(@Param("key") String key,
				   @Param("method") String method,
				   @Param("path") String path,
				   @Param("requestHash") String requestHash);

	@Modifying
	@Query("""
		UPDATE idempotency_keys
		SET status = 'completed', response_status = :responseStatus, response_body = :responseBody::jsonb, last_seen_at = now()
		WHERE idempotency_key = :key
		  AND status = 'in_progress'
		""")
	int markCompleted(@Param("key") String key, @Param("responseStatus") int responseStatus, @Param("responseBody") String responseBody);

	@Modifying
	@Query("""
		UPDATE idempotency_keys SET last_seen_at = now()
		WHERE idempotency_key = :key
		  AND status = 'in_progress'
		  AND last_seen_at < now() - (:staleSeconds || ' seconds')::interval
		""")
	int tryTakeOver(@Param("key") String key, @Param("staleSeconds") int staleSeconds);
}
