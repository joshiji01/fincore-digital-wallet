package com.fincore.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionCreatedEvent {

    private UUID referenceId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String type;
    private String status;
    private LocalDateTime createdAt;

    public TransactionCreatedEvent() {
    }

    public TransactionCreatedEvent(
            UUID referenceId,
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            String type,
            String status,
            LocalDateTime createdAt) {

        this.referenceId = referenceId;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}