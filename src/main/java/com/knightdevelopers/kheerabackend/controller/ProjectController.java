package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.knightdevelopers.kheerabackend.web.SpaceErrorHandler;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode="401",description="Authentication required",content=@Content(schema=@Schema(implementation=SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode="403",description="Action permission required",content=@Content(schema=@Schema(implementation=SpaceErrorHandler.ErrorBody.class))),
    @ApiResponse(responseCode="404",description="Resource unavailable to caller",content=@Content(schema=@Schema(implementation=SpaceErrorHandler.ErrorBody.class)))
})
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service=service; }
    @GetMapping("/spaces/{spaceId}/projects")
    @Operation(summary="List active project summaries",description="Active membership required. Literal case-insensitive name/description search (q max 100). Page 0..100000, size 1..100. Sort name, createdAt or updatedAt with asc/desc; UUID breaks ties.")
    public PageDto<ProjectSummaryDto> list(Authentication auth,@PathVariable UUID spaceId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,
            @RequestParam(defaultValue="name,asc") String sort,@RequestParam(defaultValue="") String q) {
        return service.list(auth.getName(),spaceId,page,size,sort,q);
    }
    @PostMapping("/spaces/{spaceId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create project with default board",description="Requires space.update; sprintCycleDays defaults to 7.")
    public ResponseEntity<ProjectSummaryDto> create(Authentication auth,@PathVariable UUID spaceId,@RequestBody ProjectWriteRequest request) {
        var project=service.create(auth.getName(),spaceId,request);
        return ResponseEntity.created(URI.create("/api/projects/"+project.id())).body(project);
    }
    @GetMapping("/projects/{projectId}")
    @Operation(summary="Read an active project summary",description="Requires active account, space and membership. Invisible projects return 404.")
    public ProjectSummaryDto detail(Authentication auth,@PathVariable UUID projectId) {
        return service.detail(auth.getName(),projectId);
    }
    @PatchMapping("/projects/{projectId}")
    @Operation(summary="Update project metadata",description="Requires space.update. Omission preserves values; null clears description only. Cannot change space.")
    public ProjectSummaryDto update(Authentication auth,@PathVariable UUID projectId,@RequestBody ProjectWriteRequest request) {
        return service.update(auth.getName(),projectId,request);
    }
    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Soft-delete project",description="Requires space.delete. Retains descendants but makes project, board and work-item endpoints inaccessible.")
    public ResponseEntity<Void> delete(Authentication auth,@PathVariable UUID projectId) {
        service.delete(auth.getName(),projectId);
        return ResponseEntity.noContent().build();
    }
}
