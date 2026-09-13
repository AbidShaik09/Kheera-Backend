package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.service.SpaceLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
@SecurityRequirement(name = "bearerAuth")
public class SpaceLifecycleController {
    private final SpaceLifecycleService service;
    public SpaceLifecycleController(SpaceLifecycleService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a space and its administrator membership atomically")
    public ResponseEntity<SpaceDetailDto> create(Authentication auth, @RequestBody SpaceWriteRequest request) {
        var created = service.create(auth.getName(), request);
        return ResponseEntity.created(URI.create("/api/spaces/" + created.id())).body(created);
    }
    @GetMapping("/{spaceId}")
    @Operation(summary = "Read metadata and caller capabilities for an active member")
    public SpaceDetailDto detail(Authentication auth, @PathVariable UUID spaceId) { return service.detail(auth.getName(), spaceId); }
    @PatchMapping("/{spaceId}")
    @Operation(summary = "Update space metadata", description = "Requires space.update. Omitted fields remain unchanged; null clears description/profilePic, never name.")
    public SpaceDetailDto update(Authentication auth, @PathVariable UUID spaceId, @RequestBody SpaceWriteRequest request) {
        return service.update(auth.getName(), spaceId, request);
    }
    @DeleteMapping("/{spaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a space", description = "Requires space.delete. Retains descendants; deleted spaces are inaccessible.")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID spaceId) {
        service.delete(auth.getName(), spaceId); return ResponseEntity.noContent().build();
    }
}
