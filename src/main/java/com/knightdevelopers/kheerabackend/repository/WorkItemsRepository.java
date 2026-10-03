package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;

public interface WorkItemsRepository extends JpaRepository<WorkItems, UUID> {
    String VISIBLE = """
        with recursive hidden(id) as (
            select w.id from work_items w left join work_items p on p.id=w.parent_item_id
            where w.project_id=:projectId and (w.is_deleted or p.project_id<>:projectId)
            union
            select w.id from work_items w join hidden h on w.parent_item_id=h.id where w.project_id=:projectId
        ), visible as (
            select w.id, row_number() over(partition by w.workflow_id order by w.position,w.id)-1 as board_position
            from work_items w join project_workflows s on s.id=w.workflow_id
            where w.project_id=:projectId and not w.is_deleted and not s.is_deleted
              and w.id not in (select id from hidden)
        )
        """;
    String FILTER = """
        from work_items w join project_workflows s on s.id=w.workflow_id join visible v on v.id=w.id
        where w.project_id=:projectId and not w.is_deleted and not s.is_deleted
          and w.id not in (select id from hidden)
          and (:stageId is null or w.workflow_id=:stageId)
          and (:typeId is null or w.work_item_type_id=:typeId)
          and (:memberId is null or w.assigned_to_id=:memberId)
          and (:parentId is null or w.parent_item_id=:parentId)
          and (strpos(lower(coalesce(w.title,'')),lower(:q))>0 or strpos(lower(coalesce(w.description,'')),lower(:q))>0)
        """;
    interface BoardRow {
        UUID getId();
        int getPosition();
    }
    @Query(value=VISIBLE+"select w.id as id,v.board_position as position "+FILTER+" order by s.position,w.position,w.id",
            countQuery=VISIBLE+"select count(*) "+FILTER,nativeQuery=true)
    Page<BoardRow> findFilteredIds(@Param("projectId") UUID projectId,@Param("stageId") UUID stageId,
            @Param("typeId") UUID typeId,@Param("memberId") UUID memberId,@Param("parentId") UUID parentId,
            @Param("q") String q,Pageable pageable);

    @EntityGraph(attributePaths={"project.space","workflow","workItemType","parentItem","spaceMember.user","spaceMember.spaceRole.space","spaceMember.space"})
    @Query("select w from WorkItems w where w.id in :ids")
    List<WorkItems> fetchDetails(@Param("ids") Collection<UUID> ids);

    @Query(value="""
        with recursive ancestors(id,parent_item_id,project_id,is_deleted) as (
            select id,parent_item_id,project_id,is_deleted from work_items where id=:id
            union
            select p.id,p.parent_item_id,p.project_id,p.is_deleted from work_items p join ancestors a on p.id=a.parent_item_id
        )
        select exists(select 1 from work_items w join project_workflows s on s.id=w.workflow_id
            join projects p on p.id=w.project_id join spaces sp on sp.id=p.space_id
            where w.id=:id and not w.is_deleted and not s.is_deleted and not p.is_deleted and not sp.is_deleted
            and not exists(select 1 from ancestors a where a.is_deleted or a.project_id<>w.project_id))
        """,nativeQuery=true)
    boolean isVisible(@Param("id") UUID id);

    @Query(value=VISIBLE+"select count(*) from work_items w join visible v on v.id=w.id where w.parent_item_id=:id",nativeQuery=true)
    long countActiveChildren(@Param("id") UUID id,@Param("projectId") UUID projectId);

    @Query(value=VISIBLE+"select board_position from visible where id=:id",nativeQuery=true)
    int visiblePosition(@Param("projectId") UUID projectId,@Param("id") UUID id);

    @Query(value=VISIBLE+"select id from hidden",nativeQuery=true)
    Set<UUID> hiddenIds(@Param("projectId") UUID projectId);

    @Query("select coalesce(max(w.position)+1,0) from WorkItems w where w.workflow.id=:stageId and w.isDeleted=false")
    int nextPosition(@Param("stageId") UUID stageId);

    @Query("select w from WorkItems w where w.workflow.id=:stageId and w.isDeleted=false order by w.position,w.id")
    List<WorkItems> findActiveByStage(@Param("stageId") UUID stageId);

    interface ProjectMetrics {
        UUID getProjectId();
        long getTotal();
        long getComplete();
    }
    @Query("select w.project.id as projectId, count(w) as total, " +
           "sum(case when w.workflow.isComplete=true then 1 else 0 end) as complete " +
           "from WorkItems w where w.project.id in :ids and w.isDeleted=false and w.workflow.isDeleted=false " +
           "and w.project.isDeleted=false and w.project.space.isDeleted=false group by w.project.id")
    List<ProjectMetrics> summarizeProjects(@Param("ids") Collection<UUID> ids);

    @Query("select w.project.id from WorkItems w where w.id=:id and w.isDeleted=false and w.project.isDeleted=false and w.project.space.isDeleted=false")
    Optional<UUID> findActiveProjectId(@Param("id") UUID id);

    @Query("select w from WorkItems w join fetch w.workflow where w.project.id=:projectId and w.isDeleted=false order by w.workflow.position,w.position,w.id")
    List<WorkItems> findActiveByProject(@Param("projectId") UUID projectId);

    @Query("select count(w) from WorkItems w where w.workflow.id=:stageId and w.isDeleted=false")
    long countActiveByStage(@Param("stageId") UUID stageId);
}
