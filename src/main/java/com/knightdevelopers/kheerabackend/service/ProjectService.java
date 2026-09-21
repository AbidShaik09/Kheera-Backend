package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectsRepository projects;
    private final WorkItemsRepository items;
    private final SpaceAccessService access;
    public ProjectService(ProjectsRepository projects,WorkItemsRepository items,SpaceAccessService access) {
        this.projects=projects; this.items=items; this.access=access;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageDto<ProjectSummaryDto> list(String email,UUID spaceId,int page,int size,String sort,String q) {
        access.requireSpace(email,spaceId,null,false);
        if(page<0 || page>100000) throw SpaceApiException.invalid("page","Page must be between 0 and 100000.");
        if(size<1 || size>100) throw SpaceApiException.invalid("size","Size must be between 1 and 100.");
        if(q==null || q.codePointCount(0,q.length())>100) throw SpaceApiException.invalid("q","Search must contain at most 100 characters.");
        var result=projects.findActivePage(spaceId,q.strip(),PageRequest.of(page,size,sort(sort)));
        var metrics=metrics(result.getContent().stream().map(Projects::getId).toList());
        return PageDto.from(result.map(p->dto(p,metrics.get(p.getId()))));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public ProjectSummaryDto detail(String email,UUID id) {
        authorize(email,id,null,false);
        var project=projects.findActiveById(id).orElseThrow(SpaceApiException::resourceNotFound);
        return summary(project);
    }
    @Transactional
    public ProjectSummaryDto create(String email,UUID spaceId,ProjectWriteRequest request) {
        var space=access.requireSpace(email,spaceId,SpaceAccessService.UPDATE,true);
        request.validate(true);
        var project=new Projects();
        project.assignToSpace(space);
        apply(project,request);
        projects.saveAndFlush(project); // V26 provisions the initial board in this transaction.
        return dto(project,null);
    }
    @Transactional
    public ProjectSummaryDto update(String email,UUID id,ProjectWriteRequest request) {
        authorize(email,id,SpaceAccessService.UPDATE,true);
        var project=projects.lockActiveById(id).orElseThrow(SpaceApiException::resourceNotFound);
        request.validate(false);
        apply(project,request);
        return summary(project);
    }
    @Transactional
    public void delete(String email,UUID id) {
        authorize(email,id,SpaceAccessService.DELETE,true);
        var project=projects.lockActiveById(id).orElseThrow(SpaceApiException::resourceNotFound);
        project.setDeleted(true);
        project.setUpdatedAt(Instant.now());
    }
    private void authorize(String email,UUID id,String permission,boolean lock) {
        access.requireActiveUser(email);
        UUID spaceId=projects.findActiveSpaceId(id).orElseThrow(SpaceApiException::resourceNotFound);
        access.requireSpace(email,spaceId,permission,lock);
    }
    private void apply(Projects project,ProjectWriteRequest request) {
        if(request.hasName()) project.setProjectName(request.getName());
        if(request.hasDescription()) project.setDescription(request.getDescription());
        if(request.hasSprintCycleDays()) project.setSprintCycleDays(request.getSprintCycleDays());
        if(request.hasName() || request.hasDescription() || request.hasSprintCycleDays()) project.setUpdatedAt(Instant.now());
    }
    private ProjectSummaryDto summary(Projects project) {
        return dto(project,metrics(List.of(project.getId())).get(project.getId()));
    }
    private Map<UUID,WorkItemsRepository.ProjectMetrics> metrics(List<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        return items.summarizeProjects(ids).stream().collect(Collectors.toMap(WorkItemsRepository.ProjectMetrics::getProjectId,Function.identity()));
    }
    private ProjectSummaryDto dto(Projects project,WorkItemsRepository.ProjectMetrics metrics) {
        long total=metrics==null?0:metrics.getTotal(), complete=metrics==null?0:metrics.getComplete();
        int percent=total==0?0:(int)Math.round(100.0*complete/total);
        return new ProjectSummaryDto(project.getId(),project.getSpace().getId(),project.getProjectName(),
                project.getDescription(),project.getSprintCycleDays(),percent,total-complete,project.getUpdatedAt());
    }
    private Sort sort(String value) {
        if(value==null) throw SpaceApiException.invalid("sort","Sort is required.");
        String[] parts=value.split(",",-1);
        if(parts.length!=2 || !(parts[1].equals("asc") || parts[1].equals("desc")))
            throw SpaceApiException.invalid("sort","Use field,asc or field,desc.");
        String property=switch(parts[0]) {
            case "name" -> "projectName"; case "createdAt" -> "createdAt"; case "updatedAt" -> "updatedAt";
            default -> throw SpaceApiException.invalid("sort","Allowed fields: name, createdAt, updatedAt.");
        };
        return Sort.by(Sort.Direction.fromString(parts[1]),property).and(Sort.by("id"));
    }
}
