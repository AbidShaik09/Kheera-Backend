package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.project.Projects;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ProjectsRepository extends JpaRepository<Projects, UUID> {
    @Query("select p.space.id from Projects p where p.id=:id and p.isDeleted=false and p.space.isDeleted=false")
    Optional<UUID> findActiveSpaceId(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Projects p where p.id=:id and p.isDeleted=false")
    Optional<Projects> lockActiveById(@Param("id") UUID id);
}
