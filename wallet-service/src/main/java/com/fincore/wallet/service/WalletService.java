package com.fincore.wallet.service;

import com.fincore.wallet.dto.TransactionCreatedEvent;
import com.fincore.wallet.dto.TransferRequest;
import com.fincore.wallet.dto.TransferResponse;
import com.fincore.wallet.entity.OutboxEvent;
import com.fincore.wallet.entity.Wallet;
import com.fincore.wallet.exception.InsufficientBalanceException;
import com.fincore.wallet.exception.InvalidTransferException;
import com.fincore.wallet.exception.WalletAlreadyExistsException;
import com.fincore.wallet.exception.WalletNotFoundException;
import com.fincore.wallet.repository.OutboxEventRepository;
import com.fincore.wallet.repository.WalletRepository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public WalletService(
            WalletRepository walletRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            RedisTemplate<String, String> redisTemplate) {

        this.walletRepository = walletRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Wallet createWallet(UUID userId) {

        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(
                    "Wallet already exists for user");
        }

        Wallet wallet = new Wallet(userId);

        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {

        validateAmount(amount);

        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));

        wallet.credit(amount);

        walletRepository.save(wallet);

        // Remove stale cached balance
        redisTemplate.delete("wallet:" + walletId);

        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {

        String key = "wallet:" + walletId;

        // 1. Check Redis
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            try {
                return objectMapper.readValue(
                        cached,
                        Wallet.class
                );
            } catch (Exception e) {
                // Remove corrupted cache entry
                redisTemplate.delete(key);
            }
        }

        // 2. Redis miss → PostgreSQL
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));

        // 3. Store fresh wallet in Redis
        try {
            String json = objectMapper.writeValueAsString(wallet);

            redisTemplate.opsForValue().set(
                    key,
                    json
            );

        } catch (JacksonException e) {
            // Cache failure should not break the request
        }

        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(UUID userId) {

        return walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));
    }

    @Transactional
    public TransferResponse transfer(
            TransferRequest request,
            String idempotencyKey) {

        // -----------------------------------------
        // 1. Validate idempotency key
        // -----------------------------------------

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException(
                    "Idempotency-Key is required");
        }

        String redisKey =
                "idempotency:transfer:" + idempotencyKey;

        // -----------------------------------------
        // 2. Fast Redis idempotency check
        // -----------------------------------------

        String cachedResponse =
                redisTemplate.opsForValue().get(redisKey);

        if (cachedResponse != null) {

            try {
                return objectMapper.readValue(
                        cachedResponse,
                        TransferResponse.class
                );

            } catch (Exception e) {

                redisTemplate.delete(redisKey);
            }
        }

        UUID senderId = request.getSenderWalletId();
        UUID receiverId = request.getReceiverWalletId();
        BigDecimal amount = request.getAmount();

        // -----------------------------------------
        // 3. Validate transfer
        // -----------------------------------------

        if (senderId.equals(receiverId)) {
            throw new InvalidTransferException(
                    "Sender and receiver wallets must be different");
        }

        validateAmount(amount);

        // -----------------------------------------
        // 4. Deterministic lock ordering
        // -----------------------------------------

        UUID firstId;
        UUID secondId;

        if (senderId.compareTo(receiverId) < 0) {
            firstId = senderId;
            secondId = receiverId;
        } else {
            firstId = receiverId;
            secondId = senderId;
        }

        // -----------------------------------------
        // 5. Lock both wallets
        // -----------------------------------------

        Wallet firstWallet =
                walletRepository.findByIdForUpdate(firstId)
                        .orElseThrow(() ->
                                new WalletNotFoundException(
                                        "Wallet not found"));

        Wallet secondWallet =
                walletRepository.findByIdForUpdate(secondId)
                        .orElseThrow(() ->
                                new WalletNotFoundException(
                                        "Wallet not found"));

        // -----------------------------------------
        // 6. Durable idempotency check
        // -----------------------------------------

        Optional<OutboxEvent> existingEvent =
                outboxEventRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingEvent.isPresent()) {

            try {

                TransactionCreatedEvent event =
                        objectMapper.readValue(
                                existingEvent.get().getPayload(),
                                TransactionCreatedEvent.class
                        );

                TransferResponse response =
                        new TransferResponse(
                                event.getReferenceId(),
                                event.getSenderWalletId(),
                                event.getReceiverWalletId(),
                                event.getAmount(),
                                event.getStatus()
                        );

                // Rebuild Redis cache
                redisTemplate.opsForValue().set(
                        redisKey,
                        objectMapper.writeValueAsString(response),
                        Duration.ofHours(24)
                );

                return response;

            } catch (JacksonException e) {

                throw new RuntimeException(
                        "Failed to read existing idempotency record",
                        e
                );
            }
        }

        // -----------------------------------------
        // 7. Identify sender and receiver
        // -----------------------------------------

        Wallet senderWallet;
        Wallet receiverWallet;

        if (senderId.equals(firstId)) {

            senderWallet = firstWallet;
            receiverWallet = secondWallet;

        } else {

            senderWallet = secondWallet;
            receiverWallet = firstWallet;
        }

        // -----------------------------------------
        // 8. Check balance
        // -----------------------------------------

        if (senderWallet.getBalance().compareTo(amount) < 0) {

            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        // -----------------------------------------
        // 9. Update balances
        // -----------------------------------------

        senderWallet.debit(amount);
        receiverWallet.credit(amount);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // -----------------------------------------
        // 10. Invalidate wallet cache
        // -----------------------------------------

        redisTemplate.delete(
                "wallet:" + senderWallet.getId());

        redisTemplate.delete(
                "wallet:" + receiverWallet.getId());

        // -----------------------------------------
        // 11. Create transaction event
        // -----------------------------------------

        UUID referenceId = UUID.randomUUID();

        TransactionCreatedEvent event =
                new TransactionCreatedEvent(
                        referenceId,
                        senderId,
                        receiverId,
                        amount,
                        "TRANSFER",
                        "SUCCESS",
                        LocalDateTime.now()
                );

        // -----------------------------------------
        // 12. Save outbox event
        // -----------------------------------------

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    new OutboxEvent(
                            "TransactionCreated",
                            referenceId,
                            payload,
                            idempotencyKey
                    );

            outboxEventRepository.save(outboxEvent);

        } catch (JacksonException e) {

            throw new RuntimeException(
                    "Failed to create transaction event",
                    e
            );
        }

        // -----------------------------------------
        // 13. Build response
        // -----------------------------------------

        TransferResponse response =
                new TransferResponse(
                        referenceId,
                        senderId,
                        receiverId,
                        amount,
                        "SUCCESS"
                );

        // -----------------------------------------
        // 14. Cache idempotency response
        // -----------------------------------------

        try {

            redisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(response),
                    Duration.ofHours(24)
            );

        } catch (JacksonException e) {

            throw new RuntimeException(
                    "Failed to cache idempotency response",
                    e
            );
        }

        return response;
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Amount must be greater than zero");
        }
    }
}