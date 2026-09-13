package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.knightdevelopers.kheerabackend.web.SpaceErrorHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode = "401", description = "Authentication or active account required", content = @Content(schema = @Schema(implementation = SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode = "403", description = "Insufficient action or role authority", content = @Content(schema = @Schema(implementation = SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode = "404", description = "Resource unavailable in this space", content = @Content(schema = @Schema(implementation = SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode = "409", description = "Duplicate membership or last administrator", content = @Content(schema = @Schema(implementation = SpaceErrorHandler.ErrorBody.class)))
})
public class MembershipController {
    private final MembershipService service;
    public MembershipController(MembershipService service) { this.service = service; }

    @GetMapping("/members")
    @Operation(summary = "List active space memberships", description = "Requires space.members.read. Member IDs identify memberships, not users. Size 1..100, page 0..100000, q at most 100 characters. Sort: name,email,role,createdAt,updatedAt with asc/desc; ties use membership ID.")
    public PageDto<MembershipDto> list(Authentication auth, @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "name,asc") String sort, @RequestParam(defaultValue = "") String q) {
        return service.list(auth.getName(), spaceId, page, size, sort, q);
    }
    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an existing active user", description = "Requires space.members.add and authority over target role grants. Exact trimmed account email; no invitation. Duplicate 409; deleted membership restored with same ID.")
    public MembershipDto add(Authentication auth, @PathVariable UUID spaceId, @RequestBody MembershipAddRequest request) {
        return service.add(auth.getName(), spaceId, request);
    }
    @PatchMapping("/members/{memberId}")
    @Operation(summary = "Change a membership role", description = "Requires space.members.change-role and authority over both current and target role grants. Last-administrator demotion returns 409.")
    public MembershipDto changeRole(Authentication auth, @PathVariable UUID spaceId, @PathVariable UUID memberId, @RequestBody MembershipRoleRequest request) {
        return service.changeRole(auth.getName(), spaceId, memberId, request);
    }
    @DeleteMapping("/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a membership", description = "Requires space.members.remove and authority over current role grants, including self-removal. Last administrator returns 409. Historical assignments/comments retained; access revoked.")
    public void remove(Authentication auth, @PathVariable UUID spaceId, @PathVariable UUID memberId) {
        service.remove(auth.getName(), spaceId, memberId);
    }
    @GetMapping("/roles")
    @Operation(summary = "List active roles in this space", description = "Requires space.members.read. Paginated, ordered by name and ID. No role editing.")
    public PageDto<MembershipDto.RoleDto> roles(Authentication auth, @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.roles(auth.getName(), spaceId, page, size);
    }
    @GetMapping("/permissions")
    @Operation(summary = "List active permissions in this space", description = "Requires space.members.read. Paginated catalogue, ordered by name and ID; not the caller's effective grants.")
    public PageDto<MembershipDto.PermissionDto> permissions(Authentication auth, @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.permissions(auth.getName(), spaceId, page, size);
    }
}
