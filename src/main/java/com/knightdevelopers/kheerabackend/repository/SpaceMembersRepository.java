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

    @Query(value = """
            select sm from SpaceMembers sm join fetch sm.user u join fetch sm.spaceRole r
            where sm.space.id=:spaceId and sm.space.isDeleted=false and sm.isDeleted=false
              and u.isDeleted=false and r.isDeleted=false and r.space.id=:spaceId
              and (lower(u.name) like :search escape '!' or lower(u.email) like :search escape '!')
            """, countQuery = """
            select count(sm) from SpaceMembers sm join sm.user u join sm.spaceRole r
            where sm.space.id=:spaceId and sm.space.isDeleted=false and sm.isDeleted=false
              and u.isDeleted=false and r.isDeleted=false and r.space.id=:spaceId
              and (lower(u.name) like :search escape '!' or lower(u.email) like :search escape '!')
            """)
    org.springframework.data.domain.Page<SpaceMembers> pageActive(@Param("spaceId") UUID spaceId,
            @Param("search") String search, org.springframework.data.domain.Pageable page);

    @Query("""
            select sm from SpaceMembers sm join fetch sm.user u join fetch sm.spaceRole r
            where sm.space.id=:spaceId and sm.space.isDeleted=false and sm.id=:id
              and sm.isDeleted=false and u.isDeleted=false and r.isDeleted=false and r.space.id=:spaceId
            """)
    java.util.Optional<SpaceMembers> activeTarget(@Param("spaceId") UUID spaceId, @Param("id") UUID id);

    @Query("select sm from SpaceMembers sm join fetch sm.user join fetch sm.spaceRole where sm.space.id=:spaceId and sm.user.id=:userId")
    java.util.Optional<SpaceMembers> existing(@Param("spaceId") UUID spaceId, @Param("userId") UUID userId);

    @Query(value = """
            select count(*) from space_members m
            join users u on u.id=m.user_id and u.is_deleted=false
            join spaces s on s.id=m.space_id and s.is_deleted=false
            join space_roles r on r.id=m.space_role_id and r.space_id=s.id and r.is_deleted=false
            where m.space_id=:spaceId and m.is_deleted=false and
              (select count(distinct p.permission_name) from space_role_permissions g
               join space_permissions p on p.id=g.space_permission_id
               where g.space_role_id=r.id and g.is_deleted=false and p.is_deleted=false and p.space_id=s.id
                 and p.permission_name in ('space.update','space.delete','space.members.manage'))=3
            """, nativeQuery = true)
    long countAdministrators(@Param("spaceId") UUID spaceId);

    @Query("""
            select sm from SpaceMembers sm
            join fetch sm.space s join sm.user u join fetch sm.spaceRole r
            where u.email = :email and s.id = :spaceId
              and u.isDeleted = false and sm.isDeleted = false and s.isDeleted = false
              and r.isDeleted = false and r.space.id = s.id
            """)
    java.util.Optional<SpaceMembers> findActiveMembership(@Param("email") String email, @Param("spaceId") UUID spaceId);

    @Query("""
            select distinct p.permissionName from SpaceMembers sm
            join sm.space s join sm.user u join sm.spaceRole r
            join r.rolePermissions grantEntry join grantEntry.spacePermission p
            where u.email = :email and s.id = :spaceId
              and u.isDeleted = false and sm.isDeleted = false and s.isDeleted = false
              and r.isDeleted = false and r.space.id = s.id
              and grantEntry.isDeleted = false and p.isDeleted = false and p.space.id = s.id
            """)
    java.util.Set<String> findActivePermissions(@Param("email") String email, @Param("spaceId") UUID spaceId);

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
