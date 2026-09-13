package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.space.SpaceRoles;
import com.knightdevelopers.kheerabackend.entity.space.SpacePermissions;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;

public interface MembershipRoleRepository extends JpaRepository<SpaceRoles, UUID> {
    @Query("select r from SpaceRoles r where r.id=:id and r.space.id=:spaceId and r.space.isDeleted=false and r.isDeleted=false")
    Optional<SpaceRoles> findActive(@Param("spaceId") UUID spaceId, @Param("id") UUID id);

    @Query("select r from SpaceRoles r where r.space.id=:spaceId and r.space.isDeleted=false and r.isDeleted=false")
    Page<SpaceRoles> findActiveRoles(@Param("spaceId") UUID spaceId, Pageable page);

    @Query("select p from SpacePermissions p where p.space.id=:spaceId and p.space.isDeleted=false and p.isDeleted=false")
    Page<SpacePermissions> findActivePermissions(@Param("spaceId") UUID spaceId, Pageable page);

    @Query("""
            select distinct p.permissionName from SpaceRolePermissions g
            join g.spaceRole r join g.spacePermission p
            where r.id=:roleId and r.space.id=:spaceId and r.isDeleted=false
              and r.space.isDeleted=false and g.isDeleted=false and p.isDeleted=false and p.space.id=:spaceId
            """)
    Set<String> grants(@Param("spaceId") UUID spaceId, @Param("roleId") UUID roleId);
}
