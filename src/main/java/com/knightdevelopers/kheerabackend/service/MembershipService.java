package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.space.*;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class MembershipService {
    private final SpaceAccessService access;
    private final SpaceMembersRepository members;
    private final MembershipRoleRepository roles;
    private final UserRepository users;

    public MembershipService(SpaceAccessService access, SpaceMembersRepository members, MembershipRoleRepository roles, UserRepository users) {
        this.access = access; this.members = members; this.roles = roles; this.users = users;
    }

    @Transactional(readOnly = true)
    public PageDto<MembershipDto> list(String email, UUID spaceId, int page, int size, String sort, String search) {
        access.requireSpace(email, spaceId, SpaceAccessService.MEMBER_READ, false);
        if (search == null || search.length() > 100) throw SpaceApiException.invalid("q", "Search must contain at most 100 characters.");
        String pattern = "%" + search.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return PageDto.from(members.pageActive(spaceId, pattern, page(page, size, memberSort(sort))).map(this::dto));
    }

    @Transactional(readOnly = true)
    public PageDto<MembershipDto.RoleDto> roles(String email, UUID spaceId, int page, int size) {
        access.requireSpace(email, spaceId, SpaceAccessService.MEMBER_READ, false);
        return PageDto.from(roles.findActiveRoles(spaceId, page(page, size, Sort.by("roleName", "id")))
                .map(r -> new MembershipDto.RoleDto(r.getId(), r.getRoleName())));
    }

    @Transactional(readOnly = true)
    public PageDto<MembershipDto.PermissionDto> permissions(String email, UUID spaceId, int page, int size) {
        access.requireSpace(email, spaceId, SpaceAccessService.MEMBER_READ, false);
        return PageDto.from(roles.findActivePermissions(spaceId, page(page, size, Sort.by("permissionName", "id")))
                .map(p -> new MembershipDto.PermissionDto(p.getId(), p.getPermissionName())));
    }

    @Transactional
    public MembershipDto add(String email, UUID spaceId, MembershipAddRequest request) {
        Spaces space = access.requireSpace(email, spaceId, SpaceAccessService.MEMBER_ADD, true);
        request.validate();
        SpaceRoles role = roles.findActive(spaceId, request.getRoleId()).orElseThrow(SpaceApiException::resourceNotFound);
        requireGrantSubset(access.permissions(email, spaceId), roles.grants(spaceId, role.getId()));
        // Match the existing account's exact email contract; do not invent case normalization.
        var user = users.findActiveByEmail(request.getEmail()).orElseThrow(SpaceApiException::resourceNotFound);
        var existing = members.existing(spaceId, user.getId());
        if (existing.isPresent() && !existing.get().isDeleted())
            throw SpaceApiException.conflict("DUPLICATE_MEMBERSHIP", "An active membership already exists.");
        SpaceMembers member = existing.orElseGet(() -> SpaceMembers.create(user, space, role));
        member.changeRole(role); member.setDeleted(false); member.setUpdatedAt(Instant.now());
        return dto(members.saveAndFlush(member));
    }

    @Transactional
    public MembershipDto changeRole(String email, UUID spaceId, UUID memberId, MembershipRoleRequest request) {
        access.requireSpace(email, spaceId, SpaceAccessService.MEMBER_CHANGE_ROLE, true);
        request.validate();
        SpaceMembers member = members.activeTarget(spaceId, memberId).orElseThrow(SpaceApiException::resourceNotFound);
        SpaceRoles target = roles.findActive(spaceId, request.getRoleId()).orElseThrow(SpaceApiException::resourceNotFound);
        Set<String> caller = access.permissions(email, spaceId);
        Set<String> previous = roles.grants(spaceId, member.getSpaceRole().getId());
        Set<String> next = roles.grants(spaceId, target.getId());
        requireGrantSubset(caller, previous); requireGrantSubset(caller, next);
        preserveAdministrator(spaceId, previous, next);
        member.changeRole(target); member.setUpdatedAt(Instant.now());
        return dto(member);
    }

    @Transactional
    public void remove(String email, UUID spaceId, UUID memberId) {
        access.requireSpace(email, spaceId, SpaceAccessService.MEMBER_REMOVE, true);
        SpaceMembers member = members.activeTarget(spaceId, memberId).orElseThrow(SpaceApiException::resourceNotFound);
        Set<String> previous = roles.grants(spaceId, member.getSpaceRole().getId());
        requireGrantSubset(access.permissions(email, spaceId), previous);
        preserveAdministrator(spaceId, previous, Set.of());
        member.setDeleted(true); member.setUpdatedAt(Instant.now());
    }

    private void preserveAdministrator(UUID spaceId, Set<String> previous, Set<String> next) {
        if (previous.containsAll(SpaceAccessService.ADMINISTRATOR_GRANTS)
                && !next.containsAll(SpaceAccessService.ADMINISTRATOR_GRANTS)
                && members.countAdministrators(spaceId) <= 1)
            throw SpaceApiException.conflict("LAST_ADMINISTRATOR", "At least one active administrator must remain.");
    }
    private void requireGrantSubset(Set<String> caller, Set<String> target) {
        if (!caller.containsAll(target)) throw SpaceApiException.forbidden();
    }
    private MembershipDto dto(SpaceMembers member) {
        var user = member.getUser(); var role = member.getSpaceRole();
        return new MembershipDto(member.getId(), new MembershipDto.UserSummary(user.getId(), user.getName(), user.getEmail()),
                new MembershipDto.RoleDto(role.getId(), role.getRoleName()));
    }
    private PageRequest page(int page, int size, Sort sort) {
        if (page < 0 || page > 100000) throw SpaceApiException.invalid("page", "Page must be between 0 and 100000.");
        if (size < 1 || size > 100) throw SpaceApiException.invalid("size", "Size must be between 1 and 100.");
        return PageRequest.of(page, size, sort);
    }
    private Sort memberSort(String value) {
        if (value == null) throw SpaceApiException.invalid("sort", "Sort is required.");
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || !(parts[1].equals("asc") || parts[1].equals("desc")))
            throw SpaceApiException.invalid("sort", "Use field,asc or field,desc.");
        String property = switch (parts[0]) {
            case "name" -> "user.name"; case "email" -> "user.email"; case "role" -> "spaceRole.roleName";
            case "createdAt" -> "createdAt"; case "updatedAt" -> "updatedAt";
            default -> throw SpaceApiException.invalid("sort", "Allowed fields: name, email, role, createdAt, updatedAt.");
        };
        return Sort.by(Sort.Direction.fromString(parts[1]), property).and(Sort.by("id"));
    }
}
