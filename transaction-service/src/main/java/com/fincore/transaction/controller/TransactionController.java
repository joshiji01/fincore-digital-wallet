package com.fincore.transaction.controller;

import com.fincore.transaction.entity.Transaction;
import com.fincore.transaction.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{referenceId}")
    public Transaction getTransaction(
            @PathVariable String referenceId) {

        return transactionService.getByReferenceId(referenceId);
    }

    @GetMapping("/wallet/{walletId}")
    public Page<Transaction> getWalletTransactions(
            @PathVariable UUID walletId,
            Pageable pageable) {

        return transactionService.getWalletTransactions(
                walletId,
                pageable
        );
    }
}