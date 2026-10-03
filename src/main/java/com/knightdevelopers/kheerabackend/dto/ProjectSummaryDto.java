package com.knightdevelopers.kheerabackend.dto;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectSummaryDto(UUID id, UUID spaceId, String name, String description,
        Integer sprintCycleDays,
        @Schema(description="Nearest whole percent of visible active work items in complete stages; includes parents/epics; empty projects return zero.") int progressPercent,
        @Schema(description="Visible active work items in incomplete stages, including parents/epics.") long openTaskCount,
        Instant updatedAt) { }
