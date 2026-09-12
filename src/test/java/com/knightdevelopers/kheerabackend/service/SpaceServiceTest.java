package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.SpaceListDto;
import com.knightdevelopers.kheerabackend.repository.SpaceMembersRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpaceServiceTest {

    private final SpaceMembersRepository spaceMembersRepository = mock(SpaceMembersRepository.class);
    private final SpaceService spaceService = new SpaceService(spaceMembersRepository);

    @Test
    void getSpacesForUserEmailReturnsMembershipScopedDtos() {
        String email = "member@example.com";
        List<SpaceListDto> expectedSpaces = List.of(
                new SpaceListDto(UUID.randomUUID(), "The Knight Developers")
        );
        when(spaceMembersRepository.findActiveSpacesByUserEmail(email)).thenReturn(expectedSpaces);

        List<SpaceListDto> spaces = spaceService.getSpacesForUserEmail(email);

        assertThat(spaces).isEqualTo(expectedSpaces);
        verify(spaceMembersRepository).findActiveSpacesByUserEmail(email);
    }
}
