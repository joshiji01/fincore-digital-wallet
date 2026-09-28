package com.fincore.user.dto;

import java.util.UUID;

public class UserCreatedEvent {

    private UUID eventId;
    private UUID userId;

    public UserCreatedEvent() {
    }

    public UserCreatedEvent(UUID eventId, UUID userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }
}