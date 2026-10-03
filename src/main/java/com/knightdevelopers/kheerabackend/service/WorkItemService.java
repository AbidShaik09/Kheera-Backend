package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkItemService {
    private final ProjectsRepository projects;
    private final WorkItemsRepository items;
    private final WorkItemTypesRepository types;
    private final WorkflowStageRepository stages;
    private final SpaceMembersRepository members;
    private final SpaceAccessService access;
    public WorkItemService(ProjectsRepository projects,WorkItemsRepository items,WorkItemTypesRepository types,
            WorkflowStageRepository stages,SpaceMembersRepository members,SpaceAccessService access) {
        this.projects=projects; this.items=items; this.types=types; this.stages=stages; this.members=members; this.access=access;
    }
    public record TypeDto(UUID id,String name,String icon) {}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<TypeDto> types(String email,UUID projectId) {
        authorize(email,projectId,false);
        return types.findActiveByProject(projectId).stream().map(t->new TypeDto(t.getId(),t.getName(),t.getIcon())).toList();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public WorkItemDetailDto detail(String email,UUID id) {
        authorizeItem(email,id,false);
        return detailDto(item(id));
    }
    @Transactional
    public WorkItemDetailDto create(String email,UUID projectId,WorkItemWriteRequest request) {
        var project=authorize(email,projectId,true);
        request.validate(true);
        var item=new WorkItems(); item.assignProject(project);
        apply(item,request,true);
        items.saveAndFlush(item);
        return detailDto(item);
    }
    @Transactional
    public WorkItemDetailDto update(String email,UUID id,WorkItemWriteRequest request) {
        authorizeItem(email,id,true);
        var item=item(id);
        request.validate(false);
        apply(item,request,false);
        items.flush();
        return detailDto(item);
    }
    @Transactional
    public void delete(String email,UUID id) {
        authorizeItem(email,id,true);
        var item=item(id);
        if(items.countActiveChildren(id,item.getProject().getId())>0) throw SpaceApiException.conflict("TASK_HAS_CHILDREN","Remove or reparent active children before deleting this task.");
        item.setDeleted(true); item.setUpdatedAt(Instant.now());
        compact(item.getWorkflow().getId());
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public BoardPageDto board(String email,UUID projectId,UUID stageId,UUID typeId,UUID memberId,UUID parentId,String q,String groupBy,int page,int size) {
        authorize(email,projectId,false);
        if(page<0 || page>100000) throw SpaceApiException.invalid("page","Must be between 0 and 100000.");
        if(size<1 || size>100) throw SpaceApiException.invalid("size","Must be between 1 and 100.");
        if(q==null || q.codePointCount(0,q.length())>100) throw SpaceApiException.invalid("q","At most 100 characters.");
        if(groupBy!=null && !groupBy.equals("stage")) throw SpaceApiException.invalid("groupBy","Only stage grouping is supported.");
        var columns=stages.findActiveByProject(projectId);
        if(stageId!=null && columns.stream().noneMatch(s->s.getId().equals(stageId))) throw SpaceApiException.resourceNotFound();
        // Type/member/parent filters are scoped by the base project query, including historical IDs.
        var result=items.findFilteredIds(projectId,stageId,typeId,memberId,parentId,q.strip(),PageRequest.of(page,size));
        var details=result.isEmpty()?Map.<UUID,WorkItems>of():items.fetchDetails(result.getContent().stream().map(WorkItemsRepository.BoardRow::getId).toList()).stream().collect(Collectors.toMap(WorkItems::getId,Function.identity()));
        var rows=result.getContent().stream().map(row->BoardWorkItemDto.from(details.get(row.getId()),row.getPosition())).toList();
        var groups=groupBy==null?List.<BoardPageDto.StageGroup>of():columns.stream()
                .filter(s->stageId==null || stageId.equals(s.getId()))
                .map(s->new BoardPageDto.StageGroup(new WorkflowStageDto(s.getId(),s.getWorkflowName(),s.getIcon(),s.getPosition(),s.isComplete()),
                        rows.stream().filter(i->i.stageId().equals(s.getId())).toList())).toList();
        return new BoardPageDto(rows,page,size,result.getTotalElements(),result.getTotalPages(),groups);
    }
    private WorkItemDetailDto detailDto(WorkItems item) {
        return WorkItemDetailDto.from(item,items.visiblePosition(item.getProject().getId(),item.getId()));
    }
    private Projects authorize(String email,UUID projectId,boolean mutation) {
        access.requireActiveUser(email);
        UUID space=projects.findActiveSpaceId(projectId).orElseThrow(SpaceApiException::resourceNotFound);
        access.requireSpace(email,space,mutation?SpaceAccessService.UPDATE:null,mutation);
        return (mutation?projects.lockActiveById(projectId):projects.findActiveById(projectId)).orElseThrow(SpaceApiException::resourceNotFound);
    }
    private void authorizeItem(String email,UUID id,boolean mutation) {
        access.requireActiveUser(email);
        UUID project=items.findActiveProjectId(id).orElseThrow(SpaceApiException::resourceNotFound);
        authorize(email,project,mutation);
    }
    private WorkItems item(UUID id) {
        if(!items.isVisible(id)) throw SpaceApiException.resourceNotFound();
        return items.fetchDetails(List.of(id)).stream().findFirst().orElseThrow(SpaceApiException::resourceNotFound);
    }
    private void apply(WorkItems item,WorkItemWriteRequest r,boolean create) {
        UUID project=item.getProject().getId();
        if(r.has("title")) item.setTitle(r.text("title"));
        if(r.has("description")) item.setDescription(r.text("description"));
        if(r.has("efforts")) item.setEfforts(r.efforts());
        if(create || r.has("typeId")) {
            var available=types.findActiveByProject(project);
            var type=r.has("typeId")?available.stream().filter(t->t.getId().equals(r.id("typeId"))).findFirst():available.stream().findFirst();
            item.setWorkItemType(type.orElseThrow(()->SpaceApiException.invalid("typeId","Choose an active type in this project.")));
        }
        if(create || r.has("stageId")) {
            var available=stages.findActiveByProject(project);
            var stage=(r.has("stageId")?available.stream().filter(s->s.getId().equals(r.id("stageId"))).findFirst():available.stream().findFirst())
                    .orElseThrow(()->SpaceApiException.invalid("stageId","Choose an active stage in this project."));
            if(create || !item.getWorkflow().getId().equals(stage.getId())) {
                // Existing move endpoint remains responsible for explicit insertion/reordering.
                UUID source=create?null:item.getWorkflow().getId();
                item.moveToWorkflow(stage,items.nextPosition(stage.getId()));
                if(source!=null) compact(source);
            }
        }
        if(r.has("assigneeMemberId")) {
            if(r.id("assigneeMemberId")==null) item.unassignSpaceMember();
            else item.assignToSpaceMember(members.activeTarget(item.getProject().getSpace().getId(),r.id("assigneeMemberId"))
                    .orElseThrow(()->SpaceApiException.invalid("assigneeMemberId","Choose an active member of this space.")));
        }
        if(r.has("parentId")) {
            WorkItems parent=null;
            if(r.id("parentId")!=null) {
                UUID parentProject=items.findActiveProjectId(r.id("parentId")).orElse(null);
                if(!project.equals(parentProject) || !items.isVisible(r.id("parentId")))
                    throw SpaceApiException.invalid("parentId","Choose an active parent in this project.");
                parent=items.findById(r.id("parentId")).orElseThrow(SpaceApiException::resourceNotFound);
                Set<UUID> seen=new HashSet<>();
                for(WorkItems cursor=parent;cursor!=null;cursor=cursor.getParentItem()) {
                    if(cursor.getId().equals(item.getId()) || !seen.add(cursor.getId()))
                        throw SpaceApiException.invalid("parentId","Task hierarchy cannot contain a cycle.");
                }
            }
            item.setParentItem(parent);
        }
        if(r.has("plannedStartDate")) item.setPlannedStartDate(r.date("plannedStartDate"));
        if(r.has("plannedEndDate")) item.setPlannedEndDate(r.date("plannedEndDate"));
        if(r.has("actualStartDate")) item.setActualStartDate(r.date("actualStartDate"));
        if(r.has("actualEndDate")) item.setActualEndDate(r.date("actualEndDate"));
        dates(item.getPlannedStartDate(),item.getPlannedEndDate(),"plannedEndDate");
        dates(item.getActualStartDate(),item.getActualEndDate(),"actualEndDate");
        if(!r.empty()) item.setUpdatedAt(Instant.now());
    }
    private void dates(Instant start,Instant end,String field) {
        if(start!=null && end!=null && end.isBefore(start)) throw SpaceApiException.invalid(field,"End must not precede start.");
    }
    private void compact(UUID stageId) {
        // Query auto-flushes the move/deletion; the deferred position constraint permits renumbering.
        var remaining=items.findActiveByStage(stageId);
        for(int i=0;i<remaining.size();i++) {
            if(remaining.get(i).getPosition()!=i) {
                remaining.get(i).setPosition(i);
                remaining.get(i).setUpdatedAt(Instant.now());
            }
        }
    }
}
