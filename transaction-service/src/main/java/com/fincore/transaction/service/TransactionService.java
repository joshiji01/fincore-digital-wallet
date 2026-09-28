package com.fincore.transaction.service;

import com.fincore.transaction.entity.Transaction;
import com.fincore.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Transaction getByReferenceId(String referenceId) {

        return transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found"));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getWalletTransactions(
            UUID walletId,
            Pageable pageable) {

        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(
                        walletId,
                        walletId,
                        pageable
                );
    }
}