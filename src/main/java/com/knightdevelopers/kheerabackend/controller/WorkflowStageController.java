package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.service.WorkflowStageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class WorkflowStageController {
    private final WorkflowStageService service;
    public WorkflowStageController(WorkflowStageService service) { this.service = service; }

    @GetMapping("/api/projects/{projectId}/workflow-stages")
    @Operation(summary = "List ordered active project board stages")
    public List<WorkflowStageDto> list(Authentication auth, @PathVariable UUID projectId) {
        return service.list(auth.getName(), projectId);
    }

    @GetMapping("/api/projects/{projectId}/work-items")
    @Operation(summary = "Read a paginated board, optionally filtered or grouped by stage",
            description = "groupBy=stage groups items on the current page. Global totals count active items; positions are zero-based within a stage.")
    public BoardPageDto board(Authentication auth, @PathVariable UUID projectId,
                              @RequestParam(required = false) UUID stageId,
                              @RequestParam(required = false) String groupBy,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "25") int size) {
        return service.board(auth.getName(), projectId, stageId, groupBy, page, size);
    }

    @PostMapping("/api/projects/{projectId}/workflow-stages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a board stage", description = "Requires space.update. Position omitted appends; maximum 100 active stages.")
    public WorkflowStageDto create(Authentication auth, @PathVariable UUID projectId, @RequestBody WorkflowStageRequest request) {
        return service.create(auth.getName(), projectId, request);
    }

    @PatchMapping("/api/projects/{projectId}/workflow-stages/{stageId}")
    @Operation(summary = "Rename, reorder or classify a board stage", description = "Requires space.update. Omitted fields remain unchanged.")
    public WorkflowStageDto update(Authentication auth, @PathVariable UUID projectId, @PathVariable UUID stageId, @RequestBody WorkflowStageRequest request) {
        return service.update(auth.getName(), projectId, stageId, request);
    }

    @DeleteMapping("/api/projects/{projectId}/workflow-stages/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete an empty board stage", description = "Requires space.update. Last stage and nonempty stage return 409.")
    public void delete(Authentication auth, @PathVariable UUID projectId, @PathVariable UUID stageId) {
        service.delete(auth.getName(), projectId, stageId);
    }

    @PostMapping("/api/work-items/{workItemId}/move")
    @Operation(summary = "Move a work item and atomically reorder affected columns",
            description = "Requires space.update. Zero-based insertion position; omitted appends, oversized clamps. Returns the moved item.")
    public BoardWorkItemDto move(Authentication auth, @PathVariable UUID workItemId, @RequestBody MoveWorkItemRequest request) {
        return service.move(auth.getName(), workItemId, request);
    }
}
