package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.entity.space.Spaces;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SpaceAccessService {
    public static final String UPDATE = "space.update";
    public static final String DELETE = "space.delete";
    public static final String MANAGE_MEMBERS = "space.members.manage";
    public static final String MEMBER_READ = "space.members.read";
    public static final String MEMBER_ADD = "space.members.add";
    public static final String MEMBER_CHANGE_ROLE = "space.members.change-role";
    public static final String MEMBER_REMOVE = "space.members.remove";
    public static final Set<String> ADMINISTRATOR_GRANTS = Set.of(UPDATE, DELETE, MANAGE_MEMBERS);
    public static final Set<String> CATALOGUE = Set.of(UPDATE, DELETE, MANAGE_MEMBERS,
            MEMBER_READ, MEMBER_ADD, MEMBER_CHANGE_ROLE, MEMBER_REMOVE);
    private final UserRepository users;
    private final SpaceMembersRepository members;
    private final SpacesRepository spaces;

    public SpaceAccessService(UserRepository users, SpaceMembersRepository members, SpacesRepository spaces) {
        this.users = users; this.members = members; this.spaces = spaces;
    }
    // Descendant services must call this inside their transaction before touching data.
    public Spaces requireSpace(String email, UUID spaceId, String permission, boolean lock) {
        users.findActiveByEmail(email).orElseThrow(SpaceApiException::unauthorized);
        if (lock) spaces.lockActiveById(spaceId).orElseThrow(SpaceApiException::notFound);
        var member = members.findActiveMembership(email, spaceId).orElseThrow(SpaceApiException::notFound);
        if (permission != null && !permissions(email, spaceId).contains(permission)) throw SpaceApiException.forbidden();
        return member.getSpace();
    }
    public Set<String> permissions(String email, UUID spaceId) { return members.findActivePermissions(email, spaceId); }
}
