package com.knightdevelopers.kheerabackend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public class MembershipRoleRequest {
    @Schema(type = "string", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID roleId;
    @JsonSetter("roleId") public void setRoleId(JsonNode value) {
        try {
            if (value == null || !value.isTextual()) throw new IllegalArgumentException();
            roleId = UUID.fromString(value.textValue());
            if (!roleId.toString().equalsIgnoreCase(value.textValue())) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) { throw SpaceApiException.invalid("roleId", "A valid role UUID is required."); }
    }
    @JsonAnySetter public void unknown(String key, JsonNode value) { throw SpaceApiException.invalid("body", "Unknown fields are not allowed."); }
    public UUID getRoleId() { return roleId; }
    public void validate() { if (roleId == null) throw SpaceApiException.invalid("roleId", "Role is required."); }
}
