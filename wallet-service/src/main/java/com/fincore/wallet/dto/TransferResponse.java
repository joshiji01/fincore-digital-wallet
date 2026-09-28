package com.fincore.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferResponse {

    private UUID referenceId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String status;

    public TransferResponse(
            UUID referenceId,
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            String status) {

        this.referenceId = referenceId;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
}