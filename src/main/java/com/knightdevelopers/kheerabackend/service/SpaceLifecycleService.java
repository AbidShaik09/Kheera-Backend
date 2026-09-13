package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.space.*;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class SpaceLifecycleService {
    private final SpacesRepository spaces;
    private final SpaceMembersRepository members;
    private final UserRepository users;
    private final SpaceAccessService access;
    public SpaceLifecycleService(SpacesRepository spaces, SpaceMembersRepository members, UserRepository users, SpaceAccessService access) {
        this.spaces = spaces; this.members = members; this.users = users; this.access = access;
    }
    @Transactional
    public SpaceDetailDto create(String email, SpaceWriteRequest request) {
        request.validate(true);
        var creator = users.findActiveByEmail(email).orElseThrow(SpaceApiException::unauthorized);
        Spaces space = new Spaces();
        space.setSpaceName(request.getName()); space.setDescription(request.getDescription()); space.setProfilePic(request.getProfilePic());
        // Persist permissions before cascading role grants (their FK is non-null).
        for (String name : SpaceAccessService.CATALOGUE) {
            SpacePermissions permission = new SpacePermissions(); permission.setPermissionName(name); space.addPermission(permission);
        }
        space = spaces.saveAndFlush(space);
        SpaceRoles administrator = new SpaceRoles(); administrator.setRoleName("Administrator"); space.addRole(administrator);
        for (SpacePermissions permission : space.getPermissions()) {
            SpaceRolePermissions grant = new SpaceRolePermissions(); grant.setSpaceRole(administrator); grant.setSpacePermission(permission);
            administrator.getRolePermissions().add(grant);
        }
        space = spaces.saveAndFlush(space);
        SpaceRoles memberRole = new SpaceRoles(); memberRole.setRoleName("Member"); space.addRole(memberRole);
        for (SpacePermissions permission : space.getPermissions()) {
            if (SpaceAccessService.MEMBER_READ.equals(permission.getPermissionName())) {
                SpaceRolePermissions grant = new SpaceRolePermissions(); grant.setSpaceRole(memberRole); grant.setSpacePermission(permission);
                memberRole.getRolePermissions().add(grant);
            }
        }
        space = spaces.saveAndFlush(space);
        space.addMember(creator, space.getRoles().getFirst());
        members.saveAndFlush(space.getSpaceMembers().getFirst());
        return dto(space, SpaceAccessService.CATALOGUE);
    }
    @Transactional(readOnly = true)
    public SpaceDetailDto detail(String email, UUID id) {
        return dto(access.requireSpace(email, id, null, false), access.permissions(email, id));
    }
    @Transactional
    public SpaceDetailDto update(String email, UUID id, SpaceWriteRequest request) {
        Spaces space = access.requireSpace(email, id, SpaceAccessService.UPDATE, true);
        request.validate(false);
        if (request.hasName()) space.setSpaceName(request.getName());
        if (request.hasDescription()) space.setDescription(request.getDescription());
        if (request.hasProfilePic()) space.setProfilePic(request.getProfilePic());
        if (request.hasName() || request.hasDescription() || request.hasProfilePic()) space.setUpdatedAt(Instant.now());
        return dto(space, access.permissions(email, id));
    }
    @Transactional
    public void delete(String email, UUID id) {
        Spaces space = access.requireSpace(email, id, SpaceAccessService.DELETE, true);
        space.setDeleted(true); space.setUpdatedAt(Instant.now());
    }
    private SpaceDetailDto dto(Spaces space, Set<String> permissions) {
        return new SpaceDetailDto(space.getId(), space.getSpaceName(), space.getDescription(), space.getProfilePic(),
                space.getCreatedAt(), space.getUpdatedAt(), new SpaceDetailDto.Capabilities(
                permissions.contains(SpaceAccessService.UPDATE), permissions.contains(SpaceAccessService.DELETE),
                permissions.contains(SpaceAccessService.MANAGE_MEMBERS)));
    }
}
