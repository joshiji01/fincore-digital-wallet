package com.fincore.wallet.controller;

import com.fincore.wallet.dto.TransferRequest;
import com.fincore.wallet.dto.TransferResponse;
import com.fincore.wallet.entity.Wallet;
import com.fincore.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Wallet createWallet(@RequestParam UUID userId) {
        return walletService.createWallet(userId);
    }

    @GetMapping("/{walletId}")
public Wallet getWallet(@PathVariable UUID walletId) {

  
    return walletService.getWallet(walletId);
}

    @GetMapping("/user/{userId}")
    public Wallet getWalletByUser(@PathVariable UUID userId) {
        return walletService.getWalletByUserId(userId);
    }

    @PostMapping("/{walletId}/deposit")
    public Wallet deposit(
            @PathVariable UUID walletId,
            @RequestParam @Valid @DecimalMin(value = "0.01") BigDecimal amount) {

        return walletService.deposit(walletId, amount);
    }

   @PostMapping("/transfer")
public TransferResponse transfer(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody TransferRequest request) {

    return walletService.transfer(request, idempotencyKey);
}


}