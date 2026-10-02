package com.knightdevelopers.kheerabackend.dto;

import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import java.time.Instant;
import java.util.UUID;

public record WorkItemDetailDto(UUID id, UUID projectId, UUID spaceId, String title, String description,
        Integer efforts, UUID typeId, String typeName, UUID stageId, String stageName, boolean complete,
        int position, UUID parentId, UUID assigneeMemberId, String assigneeName, boolean assigneeActive,
        Instant plannedStartDate, Instant plannedEndDate, Instant actualStartDate, Instant actualEndDate,
        Instant createdAt, Instant updatedAt) {
    public static WorkItemDetailDto from(WorkItems item) {
        var board=BoardWorkItemDto.from(item);
        return new WorkItemDetailDto(item.getId(),item.getProject().getId(),item.getProject().getSpace().getId(),
                item.getTitle(),item.getDescription(),item.getEfforts(),board.typeId(),board.typeName(),board.stageId(),
                board.stageName(),board.complete(),item.getPosition(),board.parentId(),board.assigneeMemberId(),
                board.assigneeName(),board.assigneeActive(),item.getPlannedStartDate(),item.getPlannedEndDate(),
                item.getActualStartDate(),item.getActualEndDate(),item.getCreatedAt(),item.getUpdatedAt());
    }
}
