package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.Set;
import java.util.Optional;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.entity.space.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipServiceTest {
    final SpaceAccessService access = mock(SpaceAccessService.class);
    final SpaceMembersRepository members = mock(SpaceMembersRepository.class);
    final MembershipRoleRepository roles = mock(MembershipRoleRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final MembershipService service = new MembershipService(access, members, roles, users);
    final UUID space = UUID.randomUUID();

    @Test void permissionFailures() {
        for (String permission : new String[]{SpaceAccessService.MEMBER_READ, SpaceAccessService.MEMBER_ADD,
                SpaceAccessService.MEMBER_CHANGE_ROLE, SpaceAccessService.MEMBER_REMOVE}) {
            when(access.requireSpace("actor", space, permission, !permission.equals(SpaceAccessService.MEMBER_READ)))
                    .thenThrow(SpaceApiException.forbidden());
        }
        assertThatThrownBy(() -> service.list("actor", space, 0, 25, "name,asc", "")).isInstanceOf(SpaceApiException.class);
        assertThatThrownBy(() -> service.add("actor", space, null)).isInstanceOf(SpaceApiException.class);
        assertThatThrownBy(() -> service.changeRole("actor", space, UUID.randomUUID(), null)).isInstanceOf(SpaceApiException.class);
        assertThatThrownBy(() -> service.remove("actor", space, UUID.randomUUID())).isInstanceOf(SpaceApiException.class);
        verifyNoInteractions(members, roles, users);
    }
    @Test void removePreservesLastAdministratorBeforeMutation() {
        Spaces entity = new Spaces(); entity.setId(space);
        SpaceRoles role = new SpaceRoles(); role.setId(UUID.randomUUID()); role.assignToSpace(entity);
        SpaceMembers member = SpaceMembers.create(new User("actor", "hash", "Actor"), entity, role); member.setId(UUID.randomUUID());
        when(members.activeTarget(space, member.getId())).thenReturn(Optional.of(member));
        when(access.permissions("actor", space)).thenReturn(SpaceAccessService.CATALOGUE);
        when(roles.grants(space, role.getId())).thenReturn(SpaceAccessService.CATALOGUE);
        when(members.countAdministrators(space)).thenReturn(1L);
        assertThatThrownBy(() -> service.remove("actor", space, member.getId()))
                .isInstanceOfSatisfying(SpaceApiException.class, e -> assertThat(e.code()).isEqualTo("LAST_ADMINISTRATOR"));
        assertThat(member.isDeleted()).isFalse();
        when(members.countAdministrators(space)).thenReturn(2L);
        service.remove("actor", space, member.getId());
        assertThat(member.isDeleted()).isTrue();
    }
}
