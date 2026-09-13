package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.space.Spaces;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface SpacesRepository extends JpaRepository<Spaces, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Spaces s where s.id = :id and s.isDeleted = false")
    Optional<Spaces> lockActiveById(@Param("id") UUID id);
}
