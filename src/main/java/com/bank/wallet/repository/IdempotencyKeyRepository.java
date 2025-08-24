package com.bank.wallet.repository;

import com.bank.wallet.entity.IdempotencyKey;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends CrudRepository<IdempotencyKey, String> {

	@Query("SELECT idempotency_key, method, path, request_hash, UPPER(status::text) as status, response_status, (response_body::text) as response_body, first_seen_at, last_seen_at, ref_id FROM idempotency_keys WHERE idempotency_key = :key AND request_hash = :requestHash")
	Optional<IdempotencyKey> findByIdempotencyKeyAndRequestHash(
		@Param("key") UUID key,
		@Param("requestHash") String requestHash
	);

	@Query("""
		INSERT INTO idempotency_keys (idempotency_key, method, path, request_hash, status, response_status, response_body, first_seen_at, last_seen_at, ref_id)
		VALUES (:key, :method, :path, :requestHash, 'in_progress', NULL, NULL, now(), now(), :refId)
		ON CONFLICT (idempotency_key) DO NOTHING
		RETURNING idempotency_key, method, path, request_hash, UPPER(status::text) as status, response_status, (response_body::text) as response_body, first_seen_at, last_seen_at, ref_id
		""")
	Optional<IdempotencyKey> tryInsertWithRef(
		@Param("key") UUID key,
		@Param("method") String method,
		@Param("path") String path,
		@Param("requestHash") String requestHash,
		@Param("refId") UUID refId
	);

	@Query("""
		WITH upd AS (
			UPDATE idempotency_keys
			SET last_seen_at = now()
			WHERE idempotency_key = :key
			  AND status = 'in_progress'
			  AND last_seen_at < now() - (:staleSeconds || ' seconds')::interval
			RETURNING idempotency_key, method, path, request_hash, UPPER(status::text) as status, response_status, (response_body::text) as response_body, first_seen_at, last_seen_at, ref_id
		)
		SELECT * FROM upd
		""")
	Optional<IdempotencyKey> tryTakeOverReturning(@Param("key") UUID key, @Param("staleSeconds") int staleSeconds);

	@Query("""
		WITH upd AS (
			UPDATE idempotency_keys
			SET status = :status, response_status = :responseStatus, response_body = :responseBody::jsonb, last_seen_at = now()
			WHERE idempotency_key = :key AND status = 'in_progress' AND request_hash = :requestHash
			RETURNING response_body::text AS body
		)
		SELECT body FROM upd
		""")
	Optional<String> markCompleted(
		@Param("key") UUID key,
		@Param("responseStatus") int responseStatus,
		@Param("responseBody") String responseBody,
		@Param("status") String status,
		@Param("requestHash") String requestHash
	);

}
