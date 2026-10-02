package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.workitem.WorkItemTypes;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface WorkItemTypesRepository extends JpaRepository<WorkItemTypes,UUID> {
    @Query("select t from WorkItemTypes t where t.project.id=:projectId and t.isDeleted=false order by t.name,t.id")
    List<WorkItemTypes> findActiveByProject(@Param("projectId") UUID projectId);
}
