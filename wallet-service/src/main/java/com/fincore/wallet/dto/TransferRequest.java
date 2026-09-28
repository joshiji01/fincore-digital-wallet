package com.fincore.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferRequest {

    @NotNull
    private UUID senderWalletId;

    @NotNull
    private UUID receiverWalletId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    public TransferRequest() {
    }

    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(UUID senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public void setReceiverWalletId(UUID receiverWalletId) {
        this.receiverWalletId = receiverWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}