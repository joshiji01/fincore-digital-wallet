package com.fincore.transaction.repository;

import com.fincore.transaction.entity.Transaction;
import com.fincore.transaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReferenceId(String referenceId);

    Page<Transaction> findBySenderWalletIdOrReceiverWalletId(
            UUID senderWalletId,
            UUID receiverWalletId,
            Pageable pageable
    );

    Page<Transaction> findBySenderWalletIdAndStatus(
            UUID senderWalletId,
            TransactionStatus status,
            Pageable pageable
    );
}