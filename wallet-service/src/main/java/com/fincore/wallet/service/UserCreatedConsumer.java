package com.fincore.wallet.service;

import com.fincore.wallet.dto.UserCreatedEvent;
import com.fincore.wallet.entity.Wallet;
import com.fincore.wallet.repository.WalletRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class UserCreatedConsumer {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    public UserCreatedConsumer(
            WalletRepository walletRepository,
            ObjectMapper objectMapper
    ) {
        this.walletRepository = walletRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "user-events",
            groupId = "wallet-service"
    )
    public void consume(String message) {

        try {
            UserCreatedEvent event =
                    objectMapper.readValue(
                            message,
                            UserCreatedEvent.class
                    );

            if (walletRepository.existsByUserId(event.getUserId())) {
                return;
            }

            Wallet wallet = new Wallet();

            wallet.setUserId(event.getUserId());

            walletRepository.save(wallet);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to process UserCreated event",
                    e
            );
        }
    }
}