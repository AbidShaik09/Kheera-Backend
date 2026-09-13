package com.knightdevelopers.kheerabackend.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import io.swagger.v3.oas.annotations.media.Schema;

public class MembershipAddRequest extends MembershipRoleRequest {
    @Schema(type = "string", format = "email", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    @JsonSetter("email") public void setEmail(JsonNode value) {
        if (value == null || !value.isTextual()) throw SpaceApiException.invalid("email", "Email must be a string.");
        email = value.textValue().strip();
    }
    public String getEmail() { return email; }
    @Override public void validate() {
        super.validate();
        if (email == null || email.length() > 255 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw SpaceApiException.invalid("email", "A valid email is required.");
    }
}
