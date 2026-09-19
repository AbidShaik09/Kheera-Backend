package com.knightdevelopers.kheerabackend.dto;

import java.util.List;

public record BoardPageDto(List<BoardWorkItemDto> items, int page, int size, long totalItems,
                           int totalPages, List<StageGroup> groups) {
    public record StageGroup(WorkflowStageDto stage, List<BoardWorkItemDto> items) { }
}
