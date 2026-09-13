package com.knightdevelopers.kheerabackend.dto;

import java.time.Instant;
import java.util.UUID;

public record SpaceDetailDto(UUID id, String name, String description, String profilePic,
                             Instant createdAt, Instant updatedAt, Capabilities capabilities) {
    public record Capabilities(boolean canUpdate, boolean canDelete, boolean canManageMembers) { }
}
