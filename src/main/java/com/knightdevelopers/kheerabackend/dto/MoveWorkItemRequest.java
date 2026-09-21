package com.knightdevelopers.kheerabackend.dto;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record MoveWorkItemRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID stageId,
        @Schema(minimum = "0") Integer position) {
    @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING)
    public static MoveWorkItemRequest parse(com.fasterxml.jackson.databind.JsonNode json) {
        BoardRequestFields.validateObject(json, java.util.Set.of("stageId", "position"));
        return new MoveWorkItemRequest(BoardRequestFields.uuid(json, "stageId"), BoardRequestFields.integer(json, "position"));
    }
    public void validate() {
        if (stageId == null) throw SpaceApiException.invalid("stageId", "Stage is required.");
        if (position != null && position < 0) throw SpaceApiException.invalid("position", "Position must be non-negative.");
    }
}
