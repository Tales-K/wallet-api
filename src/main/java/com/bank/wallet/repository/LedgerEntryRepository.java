package com.bank.wallet.repository;

import com.bank.wallet.entity.LedgerEntry;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends CrudRepository<LedgerEntry, UUID> {

	@Modifying
	@Query("""
		INSERT INTO ledger_entries (tx_id, wallet_id, amount, posting_type, current_balance)
		VALUES (:txId, :walletId, :amount, CAST(:postingType AS posting_type), :currentBalance)
		ON CONFLICT (tx_id, wallet_id) DO NOTHING
		""")
	int insertGeneric(
		@Param("txId") UUID txId,
		@Param("walletId") UUID walletId,
		@Param("amount") BigDecimal amount,
		@Param("postingType") String postingType,
		@Param("currentBalance") BigDecimal currentBalance
	);

	@Query("""
		SELECT current_balance FROM ledger_entries
		WHERE wallet_id = :walletId AND created_at <= :at
		ORDER BY created_at DESC
		LIMIT 1
		""")
	Optional<BigDecimal> findBalanceAsOf(@Param("walletId") UUID walletId, @Param("at") OffsetDateTime at);

	@Query("""
		SELECT ledger_id, tx_id, wallet_id, amount,
		       UPPER(posting_type::text) AS posting_type,
		       created_at, current_balance
		FROM ledger_entries
		WHERE wallet_id = :walletId
		AND (:fromTs IS NULL OR created_at >= :fromTs)
		AND (:toTs IS NULL OR created_at <= :toTs)
		ORDER BY created_at DESC
		LIMIT :limit OFFSET :offset
		""")
	List<LedgerEntry> findPage(
		@Param("walletId") UUID walletId,
		@Param("fromTs") OffsetDateTime fromTs,
		@Param("toTs") OffsetDateTime toTs,
		@Param("limit") int limit,
		@Param("offset") int offset
	);

	@Query("""
		SELECT count(*) FROM ledger_entries
		WHERE wallet_id = :walletId
		AND (:fromTs IS NULL OR created_at >= :fromTs)
		AND (:toTs IS NULL OR created_at <= :toTs)
		""")
	long countAll(
		@Param("walletId") UUID walletId,
		@Param("fromTs") OffsetDateTime fromTs,
		@Param("toTs") OffsetDateTime toTs
	);
}
