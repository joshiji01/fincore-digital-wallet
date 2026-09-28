package com.fincore.transaction.service;

import com.fincore.transaction.dto.TransactionCreatedEvent;
import com.fincore.transaction.entity.Transaction;
import com.fincore.transaction.repository.TransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(
            TransactionRepository transactionRepository,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "transaction-events",
            groupId = "transaction-service"
    )
    public void consume(String message) {

        try {
            TransactionCreatedEvent event =
                    objectMapper.readValue(
                            message,
                            TransactionCreatedEvent.class
                    );

            String referenceId = event.getReferenceId().toString();

            if (transactionRepository
                    .findByReferenceId(referenceId)
                    .isPresent()) {
                return;
            }

            Transaction transaction = new Transaction();

            transaction.setReferenceId(referenceId);
            transaction.setSenderWalletId(event.getSenderWalletId());
            transaction.setReceiverWalletId(event.getReceiverWalletId());
            transaction.setAmount(event.getAmount());
            transaction.setType(
                    com.fincore.transaction.entity.TransactionType
                            .valueOf(event.getType())
            );
            transaction.setStatus(
                    com.fincore.transaction.entity.TransactionStatus
                            .valueOf(event.getStatus())
            );
            transaction.setCreatedAt(event.getCreatedAt());

            if ("SUCCESS".equals(event.getStatus())) {
                transaction.setCompletedAt(event.getCreatedAt());
            }

            transactionRepository.save(transaction);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to process transaction event",
                    e
            );
        }
    }
}