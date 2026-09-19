package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.workitem.WorkItems;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;

public interface WorkItemsRepository extends JpaRepository<WorkItems, UUID> {
    @Query("select w.project.id from WorkItems w where w.id=:id and w.isDeleted=false and w.project.isDeleted=false and w.project.space.isDeleted=false")
    Optional<UUID> findActiveProjectId(@Param("id") UUID id);

    @Query("select w from WorkItems w join fetch w.workflow where w.project.id=:projectId and w.isDeleted=false order by w.workflow.position,w.position,w.id")
    List<WorkItems> findActiveByProject(@Param("projectId") UUID projectId);

    @Query(value="select w from WorkItems w join fetch w.workflow where w.project.id=:projectId and w.isDeleted=false and w.workflow.isDeleted=false and (:stageId is null or w.workflow.id=:stageId) order by w.workflow.position,w.position,w.id",
        countQuery="select count(w) from WorkItems w where w.project.id=:projectId and w.isDeleted=false and w.workflow.isDeleted=false and (:stageId is null or w.workflow.id=:stageId)")
    Page<WorkItems> findBoard(@Param("projectId") UUID projectId, @Param("stageId") UUID stageId, Pageable pageable);

    @Query("select count(w) from WorkItems w where w.workflow.id=:stageId and w.isDeleted=false")
    long countActiveByStage(@Param("stageId") UUID stageId);
}
