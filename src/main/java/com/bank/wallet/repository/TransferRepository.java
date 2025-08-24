package com.bank.wallet.repository;

import com.bank.wallet.entity.Transfer;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface TransferRepository extends CrudRepository<Transfer, UUID> {

	@Modifying
	@Query("""
		INSERT INTO transfers (transfer_id, from_wallet_id, to_wallet_id, amount, created_at)
		VALUES (:transferId, :fromWallet, :toWallet, :amount, now())
		ON CONFLICT (transfer_id) DO NOTHING
		""")
	int insertIfAbsent(@Param("transferId") UUID transferId,
	                   @Param("fromWallet") UUID fromWallet,
	                   @Param("toWallet") UUID toWallet,
	                   @Param("amount") BigDecimal amount);
}
