package com.bank.wallet.repository;

import com.bank.wallet.entity.LedgerEntry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends CrudRepository<LedgerEntry, UUID> {
}
