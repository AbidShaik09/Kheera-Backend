package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.project.ProjectWorkflows;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface WorkflowStageRepository extends JpaRepository<ProjectWorkflows, UUID> {
    @Query("select w from ProjectWorkflows w where w.project.id=:projectId and w.isDeleted=false order by w.position, w.id")
    List<ProjectWorkflows> findActiveByProject(@Param("projectId") UUID projectId);
}
