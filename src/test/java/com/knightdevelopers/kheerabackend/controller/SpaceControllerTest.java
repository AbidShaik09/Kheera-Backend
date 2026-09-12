package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.SpaceListDto;
import com.knightdevelopers.kheerabackend.service.SpaceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpaceControllerTest {

    private final SpaceService spaceService = mock(SpaceService.class);
    private final SpaceController spaceController = new SpaceController(spaceService);

    @Test
    void getSpacesUsesJwtEmailPrincipalInsteadOfParsingUuid() {
        String emailPrincipal = "member@example.com";
        List<SpaceListDto> expectedSpaces = List.of(
                new SpaceListDto(UUID.randomUUID(), "The Knight Developers")
        );
        when(spaceService.getSpacesForUserEmail(emailPrincipal)).thenReturn(expectedSpaces);

        ResponseEntity<?> response = spaceController.getSpaces(
                new UsernamePasswordAuthenticationToken(emailPrincipal, null, List.of())
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedSpaces);
        verify(spaceService).getSpacesForUserEmail(emailPrincipal);
    }

    @Test
    void getSpacesRejectsMissingAuthentication() {
        ResponseEntity<List<SpaceListDto>> response = spaceController.getSpaces(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }
}
