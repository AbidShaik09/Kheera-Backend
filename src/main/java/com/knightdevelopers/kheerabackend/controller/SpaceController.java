package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.SpaceListDto;
import com.knightdevelopers.kheerabackend.service.SpaceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/spaces")
public class SpaceController {
    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public ResponseEntity<List<SpaceListDto>> getSpaces(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = authentication.getName();
        List<SpaceListDto> spaces = spaceService.getSpacesForUserEmail(userEmail);
        return ResponseEntity.ok(spaces);
    }
}
