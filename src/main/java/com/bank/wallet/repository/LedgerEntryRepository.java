package com.bank.wallet.repository;

import com.bank.wallet.entity.LedgerEntry;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends CrudRepository<LedgerEntry, UUID> {

	@Modifying
	@Query("""
		INSERT INTO ledger_entries (tx_id, wallet_id, amount, posting_type)
		VALUES (:txId, :walletId, :amount, 'deposit'::posting_type)
		""")
	void insertDeposit(@Param("txId") UUID txId,
	                  @Param("walletId") UUID walletId,
	                  @Param("amount") BigDecimal amount);

	@Modifying
	@Query("""
		INSERT INTO ledger_entries (tx_id, wallet_id, amount, posting_type)
		VALUES (:txId, :walletId, :amount, 'withdraw'::posting_type)
		""")
	void insertWithdraw(@Param("txId") UUID txId,
	                   @Param("walletId") UUID walletId,
	                   @Param("amount") BigDecimal amount);
}
