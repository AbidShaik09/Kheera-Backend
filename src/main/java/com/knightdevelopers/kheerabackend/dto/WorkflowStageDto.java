package com.knightdevelopers.kheerabackend.dto;
import java.util.UUID;
public record WorkflowStageDto(UUID id, String name, String icon, int position, boolean complete) { }
