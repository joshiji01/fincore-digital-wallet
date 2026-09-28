package com.fincore.wallet.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "outbox_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_outbox_idempotency_key",
            columnNames = "idempotency_key"
        )
    },
    indexes = {
        @Index(name = "idx_outbox_processed", columnList = "processed")
    }
)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    public OutboxEvent() {
    }

    public OutboxEvent(
        String eventType,
        UUID aggregateId,
        String payload,
        String idempotencyKey) {

    this.eventType = eventType;
    this.aggregateId = aggregateId;
    this.payload = payload;
    this.idempotencyKey = idempotencyKey;
}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    public String getIdempotencyKey() {
    return idempotencyKey;
}

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}