package com.knightdevelopers.kheerabackend.dto;

import java.util.UUID;

public record MembershipDto(UUID id, UserSummary user, RoleDto role) {
    public record UserSummary(UUID id, String name, String email) { }
    public record RoleDto(UUID id, String name) { }
    public record PermissionDto(UUID id, String name) { }
}
