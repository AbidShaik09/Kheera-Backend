package com.knightdevelopers.kheerabackend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

@Schema(description = "Space metadata. PATCH omission preserves a value; null clears optional fields. Unknown fields are rejected.")
public class SpaceWriteRequest {
    @Schema(type = "string", maxLength = 255, description = "Trimmed, nonblank name. Required for creation; cannot be null.")
    private String name;
    @Schema(type = "string", maxLength = 500, nullable = true)
    private String description;
    @Schema(type = "string", maxLength = 255, nullable = true, description = "Absolute HTTP(S) image URL without credentials.")
    private String profilePic;
    @Schema(hidden = true) private boolean namePresent;
    @Schema(hidden = true) private boolean descriptionPresent;
    @Schema(hidden = true) private boolean profilePicPresent;

    @JsonSetter("name") public void setName(JsonNode value) { namePresent = true; name = text("name", value); }
    @JsonSetter("description") public void setDescription(JsonNode value) { descriptionPresent = true; description = text("description", value); }
    @JsonSetter("profilePic") public void setProfilePic(JsonNode value) { profilePicPresent = true; profilePic = text("profilePic", value); }
    @JsonAnySetter public void unknown(String key, JsonNode value) { throw SpaceApiException.invalid("body", "Unknown fields are not allowed."); }

    private static String text(String field, JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw SpaceApiException.invalid(field, "Must be a string or null.");
        return value.textValue();
    }

    public void validate(boolean creation) {
        if (creation || namePresent) {
            if (name == null || name.isBlank()) throw SpaceApiException.invalid("name", "Name is required.");
            name = name.strip();
            limit("name", name, 255);
        }
        limit("description", description, 500);
        limit("profilePic", profilePic, 255);
        if (profilePic != null) {
            try {
                URI uri = URI.create(profilePic);
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw SpaceApiException.invalid("profilePic", "Must be an absolute HTTP(S) URL without credentials.");
            }
        }
    }

    private static void limit(String field, String value, int max) {
        if (value != null && value.codePointCount(0, value.length()) > max)
            throw SpaceApiException.invalid(field, "Must contain at most " + max + " characters.");
    }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getProfilePic() { return profilePic; }
    public boolean hasName() { return namePresent; }
    public boolean hasDescription() { return descriptionPresent; }
    public boolean hasProfilePic() { return profilePicPresent; }
}
