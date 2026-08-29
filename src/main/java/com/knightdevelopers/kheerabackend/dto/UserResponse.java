package com.knightdevelopers.kheerabackend.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserResponse {

    private UUID id;
    private String name;
    private String email;

    public UserResponse(UUID id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

}