package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.project.*;
import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class WorkflowStageService {
    private final ProjectsRepository projects;
    private final WorkflowStageRepository stages;
    private final WorkItemsRepository workItems;
    private final SpaceAccessService access;

    public WorkflowStageService(ProjectsRepository projects, WorkflowStageRepository stages,
                                WorkItemsRepository workItems, SpaceAccessService access) {
        this.projects = projects;
        this.stages = stages;
        this.workItems = workItems;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<WorkflowStageDto> list(String email, UUID projectId) {
        authorize(email, projectId, false);
        return stages.findActiveByProject(projectId).stream().map(this::dto).toList();
    }

    @Transactional
    public WorkflowStageDto create(String email, UUID projectId, WorkflowStageRequest request) {
        authorize(email, projectId, true);
        request.validate(true);
        var all = stages.findActiveByProject(projectId);
        if (all.size() >= 100) throw SpaceApiException.conflict("STAGE_LIMIT", "A project can have at most 100 active stages.");
        var stage = new ProjectWorkflows();
        stage.assignProject(projects.getReferenceById(projectId));
        stage.setWorkflowName(request.name().strip());
        stage.setIcon(request.icon());
        stage.setComplete(Boolean.TRUE.equals(request.complete()));
        all.add(insertionPosition(request.position(), all.size()), stage);
        renumberStages(all);
        stages.save(stage);
        return dto(stage);
    }

    @Transactional
    public WorkflowStageDto update(String email, UUID projectId, UUID stageId, WorkflowStageRequest request) {
        authorize(email, projectId, true);
        request.validate(false);
        var all = stages.findActiveByProject(projectId);
        var stage = findStage(all, stageId);
        if (request.name() != null) stage.setWorkflowName(request.name().strip());
        if (request.icon() != null) stage.setIcon(request.icon());
        if (request.complete() != null) stage.setComplete(request.complete());
        if (request.position() != null) {
            all.remove(stage);
            all.add(insertionPosition(request.position(), all.size()), stage);
        }
        stage.setUpdatedAt(Instant.now());
        renumberStages(all);
        return dto(stage);
    }

    @Transactional
    public void delete(String email, UUID projectId, UUID stageId) {
        authorize(email, projectId, true);
        var all = stages.findActiveByProject(projectId);
        var stage = findStage(all, stageId);
        if (all.size() == 1) throw SpaceApiException.conflict("LAST_STAGE", "A project must retain one active stage.");
        var remainingItems = workItems.findActiveByStage(stageId);
        var hidden = workItems.hiddenIds(projectId);
        if (remainingItems.stream().anyMatch(w -> !hidden.contains(w.getId())))
            throw SpaceApiException.conflict("STAGE_NOT_EMPTY", "Move active work items before deleting this stage.");
        if (!remainingItems.isEmpty()) {
            var fallback = all.stream().filter(s -> !s.getId().equals(stageId)).findFirst().orElseThrow();
            int position = workItems.nextPosition(fallback.getId());
            for (var item : remainingItems) {
                item.moveToWorkflow(fallback, position++);
                item.setUpdatedAt(Instant.now());
            }
            // The database protects nonempty stages; flush relocation before deleting the stage.
            workItems.flush();
        }
        stage.setDeleted(true);
        stage.setUpdatedAt(Instant.now());
        all.remove(stage);
        renumberStages(all);
    }

    @Transactional
    public BoardWorkItemDto move(String email, UUID workItemId, MoveWorkItemRequest request) {
        // Discover ownership without loading an entity before the lock: membership revocation
        // and deletion use the same space lock. Project lock is always acquired second.
        UUID projectId = workItems.findActiveProjectId(workItemId).orElseThrow(SpaceApiException::resourceNotFound);
        authorize(email, projectId, true);
        request.validate();
        if (!workItems.isVisible(workItemId)) throw SpaceApiException.resourceNotFound();
        var target = findStage(stages.findActiveByProject(projectId), request.stageId());
        var all = workItems.findActiveByProject(projectId);
        var hidden = workItems.hiddenIds(projectId);
        var item = all.stream().filter(w -> w.getId().equals(workItemId)).findFirst()
                .orElseThrow(SpaceApiException::resourceNotFound);
        UUID sourceId = item.getWorkflow().getId();
        var source = new ArrayList<>(all.stream().filter(w -> w.getWorkflow().getId().equals(sourceId) && !w.getId().equals(workItemId) && !hidden.contains(w.getId())).toList());
        var destination = sourceId.equals(target.getId()) ? source :
                new ArrayList<>(all.stream().filter(w -> w.getWorkflow().getId().equals(target.getId()) && !hidden.contains(w.getId())).toList());
        int position = insertionPosition(request.position(), destination.size());
        destination.add(position, item);
        // Keep hidden legacy rows after visible rows so physical positions remain unique.
        source.addAll(all.stream().filter(w -> w.getWorkflow().getId().equals(sourceId) && hidden.contains(w.getId())).toList());
        if (destination != source) destination.addAll(all.stream().filter(w -> w.getWorkflow().getId().equals(target.getId()) && hidden.contains(w.getId())).toList());
        item.moveToWorkflow(target, 0);
        renumberItems(source);
        if (destination != source) renumberItems(destination);
        item.setUpdatedAt(Instant.now());
        return BoardWorkItemDto.from(item, position);
    }

    private void authorize(String email, UUID projectId, boolean mutation) {
        UUID spaceId = projects.findActiveSpaceId(projectId).orElseThrow(SpaceApiException::resourceNotFound);
        access.requireSpace(email, spaceId, mutation ? SpaceAccessService.UPDATE : null, mutation);
        if (mutation) projects.lockActiveById(projectId).orElseThrow(SpaceApiException::resourceNotFound);
    }

    private ProjectWorkflows findStage(List<ProjectWorkflows> all, UUID id) {
        return all.stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow(SpaceApiException::resourceNotFound);
    }

    private int insertionPosition(Integer requested, int size) {
        return requested == null ? size : Math.min(requested, size);
    }

    private void renumberStages(List<ProjectWorkflows> all) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPosition() != i) all.get(i).setUpdatedAt(Instant.now());
            all.get(i).setPosition(i);
        }
    }

    private void renumberItems(List<WorkItems> all) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPosition() != i) all.get(i).setUpdatedAt(Instant.now());
            all.get(i).setPosition(i);
        }
    }

    private WorkflowStageDto dto(ProjectWorkflows s) {
        return new WorkflowStageDto(s.getId(), s.getWorkflowName(), s.getIcon(), s.getPosition(), s.isComplete());
    }
}
