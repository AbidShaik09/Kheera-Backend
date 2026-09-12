package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.dto.SpaceListDto;
import com.knightdevelopers.kheerabackend.entity.space.SpaceMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpaceMembersRepository extends JpaRepository<SpaceMembers, UUID> {

    @Query("""
            select new com.knightdevelopers.kheerabackend.dto.SpaceListDto(s.id, s.spaceName)
            from SpaceMembers sm
            join sm.space s
            join sm.user u
            where u.email = :email
              and u.isDeleted = false
              and sm.isDeleted = false
              and s.isDeleted = false
            order by s.spaceName asc
            """)
    List<SpaceListDto> findActiveSpacesByUserEmail(@Param("email") String email);
}
