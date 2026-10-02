package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.service.WorkItemService;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

@RestController
@SecurityRequirement(name="bearerAuth")
public class WorkItemController {
    private final WorkItemService service;
    public WorkItemController(WorkItemService service) { this.service=service; }
    @GetMapping("/api/projects/{projectId}/work-item-types")
    @Operation(summary="List active project task types",description="Existing/new projects without active types are provisioned with Task. UUIDs are project-scoped.")
    public List<WorkItemService.TypeDto> types(Authentication auth,@PathVariable UUID projectId) { return service.types(auth.getName(),projectId); }
    @GetMapping("/api/work-items/{workItemId}")
    @Operation(summary="Read an authorized task",description="Task/project/space and task ancestors must be active; assignment uses membership IDs. Children use the board parentId filter.")
    public WorkItemDetailDto detail(Authentication auth,@PathVariable UUID workItemId) { return service.detail(auth.getName(),workItemId); }
    @PostMapping("/api/projects/{projectId}/work-items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create a task",description="Requires space.update. Title required; defaults: efforts=1, first active stage/type. Sprint fields unsupported.")
    public ResponseEntity<WorkItemDetailDto> create(Authentication auth,@PathVariable UUID projectId,@RequestBody WorkItemWriteRequest request) {
        var task=service.create(auth.getName(),projectId,request);
        return ResponseEntity.created(URI.create("/api/work-items/"+task.id())).body(task);
    }
    @PatchMapping("/api/work-items/{workItemId}")
    @Operation(summary="Edit a task",description="Requires space.update. Omission preserves; null clears description, parentId, assigneeMemberId and dates. Cycles and invalid dates/effort rejected. A stage change appends; explicit reordering uses /move.")
    public WorkItemDetailDto update(Authentication auth,@PathVariable UUID workItemId,@RequestBody WorkItemWriteRequest request) { return service.update(auth.getName(),workItemId,request); }
    @DeleteMapping("/api/work-items/{workItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Soft-delete a task",description="Requires space.update. Active children cause 409 TASK_HAS_CHILDREN; remove or reparent first. Historical content is retained.")
    public void delete(Authentication auth,@PathVariable UUID workItemId) { service.delete(auth.getName(),workItemId); }
    @GetMapping("/api/projects/{projectId}/work-items")
    @Operation(summary="Read a filtered task board",description="AND filters; literal case-insensitive title/description search; assigneeMemberId is a membership UUID. page 0..100000, size 1..100, q at most 100 characters. groupBy=stage groups current-page items; totals cover all matching tasks. Sprint/assigneeId and unknown parameters rejected.")
    public BoardPageDto board(Authentication auth,@PathVariable UUID projectId,
            @RequestParam(required=false) UUID stageId,@RequestParam(required=false) UUID typeId,
            @RequestParam(required=false) UUID assigneeMemberId,@RequestParam(required=false) UUID parentId,
            @RequestParam(defaultValue="") String q,@RequestParam(required=false) String groupBy,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,
            @RequestParam Map<String,String> parameters) {
        if(!Set.of("stageId","typeId","assigneeMemberId","parentId","q","groupBy","page","size").containsAll(parameters.keySet()))
            throw SpaceApiException.invalid("query","Unknown filter; sprintId and assigneeId are unsupported.");
        return service.board(auth.getName(),projectId,stageId,typeId,assigneeMemberId,parentId,q,groupBy,page,size);
    }
}
