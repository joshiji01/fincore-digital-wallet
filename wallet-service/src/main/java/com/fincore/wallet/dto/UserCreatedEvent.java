package com.fincore.wallet.dto;

import java.util.UUID;

public class UserCreatedEvent {

    private UUID eventId;
    private UUID userId;

    public UserCreatedEvent() {
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }
}