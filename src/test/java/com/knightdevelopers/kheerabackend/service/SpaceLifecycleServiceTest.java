package com.knightdevelopers.kheerabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.dto.SpaceWriteRequest;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.entity.space.*;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class SpaceLifecycleServiceTest {
    final SpacesRepository spaces = mock(SpacesRepository.class);
    final SpaceMembersRepository members = mock(SpaceMembersRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final SpaceAccessService access = mock(SpaceAccessService.class);
    final SpaceLifecycleService service = new SpaceLifecycleService(spaces, members, users, access);

    SpaceWriteRequest input(String json) throws Exception {
        return new ObjectMapper().readValue(json, SpaceWriteRequest.class);
    }

    @Test void createBootstrapsCreatorAndAllGrants() throws Exception {
        User creator = new User("owner@example.com", "hash", "Owner");
        when(users.findActiveByEmail(creator.getEmail())).thenReturn(Optional.of(creator));
        when(spaces.saveAndFlush(any())).thenAnswer(call -> {
            Spaces space = call.getArgument(0);
            space.setId(UUID.randomUUID());
            assertThat(space.getPermissions()).extracting(SpacePermissions::getPermissionName)
                    .containsExactlyInAnyOrderElementsOf(SpaceAccessService.CATALOGUE);
            return space;
        });
        var result = service.create(creator.getEmail(), input("{\"name\":\"  Product  \"}"));
        assertThat(result.name()).isEqualTo("Product");
        assertThat(result.capabilities().canDelete()).isTrue();
        verify(members).saveAndFlush(argThat(member -> member.getUser() == creator
                && member.getSpaceRole().getSpace() == member.getSpace()
                && member.getSpaceRole().getRoleName().equals("Administrator")
                && member.getSpaceRole().getRolePermissions().size() == 7));
    }

    @Test void inactiveCreatorCannotCreate() throws Exception {
        var request = input("{\"name\":\"Product\"}");
        assertThatThrownBy(() -> service.create("deleted@example.com", request))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(401));
        verifyNoInteractions(spaces, members);
    }

    @Test void patchPreservesOmissionsAndClearsExplicitNulls() throws Exception {
        Spaces space = new Spaces(); space.setId(UUID.randomUUID()); space.setSpaceName("Old");
        space.setDescription("Keep"); space.setProfilePic("https://example.com/a.png");
        when(access.requireSpace("owner", space.getId(), "space.update", true)).thenReturn(space);
        when(access.permissions("owner", space.getId())).thenReturn(Set.of("space.update"));
        var result = service.update("owner", space.getId(), input("{\"profilePic\":null}"));
        assertThat(result.name()).isEqualTo("Old");
        assertThat(result.description()).isEqualTo("Keep");
        assertThat(result.profilePic()).isNull();
    }

    @Test void deleteOnlySoftDeletesAuthorizedSpace() {
        Spaces space = new Spaces(); space.setId(UUID.randomUUID());
        when(access.requireSpace("owner", space.getId(), "space.delete", true)).thenReturn(space);
        service.delete("owner", space.getId());
        assertThat(space.isDeleted()).isTrue();
        verifyNoInteractions(members);
        verify(spaces, never()).delete(any());
    }

    @Test void rejectsInvalidFieldsBeforePersistence() throws Exception {
        for (String json : List.of("{}", "{\"name\":null}", "{\"name\":\"  \"}",
                "{\"name\":\"" + "a".repeat(256) + "\"}",
                "{\"name\":\"A\",\"description\":\"" + "a".repeat(501) + "\"}",
                "{\"name\":\"A\",\"profilePic\":\"javascript:alert(1)\"}",
                "{\"name\":\"A\",\"profilePic\":\"https://user:pass@example.com/a\"}")) {
            var request = input(json);
            assertThatThrownBy(() -> service.create("owner", request)).isInstanceOf(SpaceApiException.class);
        }
        verifyNoInteractions(spaces, members);
    }

    @Test void rejectsUnknownFieldsAndNonStringValues() {
        for (String json : List.of("{\"ownerId\":\"forged\"}", "{\"name\":12}", "{\"description\":{}}")) {
            assertThatThrownBy(() -> input(json)).isInstanceOf(Exception.class);
        }
    }
}
