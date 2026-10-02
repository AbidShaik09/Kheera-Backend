package com.knightdevelopers.kheerabackend.dto;

import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import java.util.UUID;

public record BoardWorkItemDto(UUID id, UUID projectId, String title, UUID stageId,
                               String stageName, boolean complete, int position,
                               UUID typeId, String typeName, UUID parentId, UUID assigneeMemberId,
                               String assigneeName, boolean assigneeActive) {
    public static BoardWorkItemDto from(WorkItems item) {
        var stage = item.getWorkflow();
        var type = item.getWorkItemType();
        var member = item.getSpaceMember();
        boolean active = member != null && !member.isDeleted() && !member.getUser().isDeleted()
                && !member.getSpaceRole().isDeleted() && !member.getSpace().isDeleted()
                && member.getSpaceRole().getSpace().getId().equals(member.getSpace().getId());
        return new BoardWorkItemDto(item.getId(), item.getProject().getId(), item.getTitle(),
                stage.getId(), stage.getWorkflowName(), stage.isComplete(), item.getPosition(),
                type == null ? null : type.getId(), type == null ? null : type.getName(),
                item.getParentItem() == null ? null : item.getParentItem().getId(),
                member == null ? null : member.getId(), member == null ? null : member.getUser().getName(), active);
    }
}
