package com.fincore.wallet.repository;

import com.fincore.wallet.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();
    Optional<OutboxEvent> findByIdempotencyKey(String idempotencyKey);
}