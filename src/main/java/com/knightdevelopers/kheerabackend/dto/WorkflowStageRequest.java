package com.knightdevelopers.kheerabackend.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
public record WorkflowStageRequest(@Schema(maxLength = 100) String name,
                                   @Schema(maxLength = 255) String icon,
                                   @Schema(minimum = "0") Integer position, Boolean complete) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WorkflowStageRequest parse(JsonNode json) {
        BoardRequestFields.validateObject(json, Set.of("name", "icon", "position", "complete"));
        return new WorkflowStageRequest(BoardRequestFields.string(json, "name"), BoardRequestFields.string(json, "icon"),
                BoardRequestFields.integer(json, "position"), BoardRequestFields.bool(json, "complete"));
    }
    public void validate(boolean creating) {
        if (creating && name == null) throw SpaceApiException.invalid("name", "Name is required.");
        if (name != null && (name.strip().isEmpty() || name.strip().length() > 100)) throw SpaceApiException.invalid("name", "Name must be 1 to 100 characters.");
        if (icon != null && icon.length() > 255) throw SpaceApiException.invalid("icon", "Icon must be at most 255 characters.");
        if (position != null && position < 0) throw SpaceApiException.invalid("position", "Position must be non-negative.");
    }
}
