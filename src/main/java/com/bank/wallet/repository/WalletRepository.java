package com.bank.wallet.repository;

import com.bank.wallet.entity.Wallet;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, UUID> {

	/**
	 * Deposit funds and return new balance atomically
	 */
	@Query("""
		UPDATE wallets
		SET current_balance = current_balance + :amount, updated_at = now()
		WHERE wallet_id = :walletId
		RETURNING current_balance
		""")
	Optional<BigDecimal> depositAndGetNewBalance(@Param("walletId") UUID walletId, @Param("amount") BigDecimal amount);

	/**
	 * Withdraw funds with overdraft protection and return new balance atomically
	 */
	@Query("""
		UPDATE wallets
		SET current_balance = current_balance - :amount, updated_at = now()
		WHERE wallet_id = :walletId AND current_balance >= :amount
		RETURNING current_balance
		""")
	Optional<BigDecimal> withdrawAndGetNewBalance(@Param("walletId") UUID walletId, @Param("amount") BigDecimal amount);
}
