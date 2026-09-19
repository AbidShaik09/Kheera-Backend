package com.knightdevelopers.kheerabackend.dto;

import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import java.util.UUID;

public record BoardWorkItemDto(UUID id, UUID projectId, String title, UUID stageId,
                               String stageName, boolean complete, int position) {
    public static BoardWorkItemDto from(WorkItems item) {
        var stage = item.getWorkflow();
        return new BoardWorkItemDto(item.getId(), item.getProject().getId(), item.getTitle(),
                stage.getId(), stage.getWorkflowName(), stage.isComplete(), item.getPosition());
    }
}
