package com.knightdevelopers.kheerabackend.dto;

import java.util.UUID;

public class SpaceListDto {
    private UUID id;
    private String name;

    public SpaceListDto(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
