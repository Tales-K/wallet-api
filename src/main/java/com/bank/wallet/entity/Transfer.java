package com.bank.wallet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @Column("transfer_id")
    private UUID transferId;

    @Column("from_wallet_id")
    private UUID fromWalletId;

    @Column("to_wallet_id")
    private UUID toWalletId;

    @Column("amount")
    private BigDecimal amount;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    /**
     * Validates that the transfer is not between the same wallet
     */
    public void validateTransfer() {
        if (fromWalletId != null && fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
    }
}
