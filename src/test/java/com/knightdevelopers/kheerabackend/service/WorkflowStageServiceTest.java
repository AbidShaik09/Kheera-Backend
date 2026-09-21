package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.project.*;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowStageServiceTest {
    ProjectsRepository projects=mock(ProjectsRepository.class);
    WorkflowStageRepository stages=mock(WorkflowStageRepository.class);
    WorkItemsRepository items=mock(WorkItemsRepository.class);
    SpaceAccessService access=mock(SpaceAccessService.class);
    WorkflowStageService service=new WorkflowStageService(projects,stages,items,access);

    @Test void locksSpaceBeforeProjectAndDoesNotTouchBoardWhenPermissionDenied() {
        UUID p=UUID.randomUUID(), space=UUID.randomUUID();
        when(projects.findActiveSpaceId(p)).thenReturn(Optional.of(space));
        doThrow(SpaceApiException.forbidden()).when(access).requireSpace("caller",space,SpaceAccessService.UPDATE,true);
        assertThatThrownBy(()->service.create("caller",p,new WorkflowStageRequest("Stage",null,null,false))).isInstanceOf(SpaceApiException.class);
        verify(projects,never()).lockActiveById(any());
        verifyNoInteractions(stages,items);
    }

    @Test void preservesStageCompletionWhenOnlyRenaming() {
        UUID p=UUID.randomUUID(), space=UUID.randomUUID();
        when(projects.findActiveSpaceId(p)).thenReturn(Optional.of(space));
        when(projects.lockActiveById(p)).thenReturn(Optional.of(new Projects()));
        ProjectWorkflows stage=new ProjectWorkflows(); stage.setId(UUID.randomUUID()); stage.setComplete(true);
        when(stages.findActiveByProject(p)).thenReturn(new ArrayList<>(List.of(stage)));
        var result=service.update("caller",p,stage.getId(),new WorkflowStageRequest("Renamed",null,null,null));
        assertThat(result.complete()).isTrue();
        assertThat(result.name()).isEqualTo("Renamed");
        var order=inOrder(access,projects);
        order.verify(access).requireSpace("caller",space,SpaceAccessService.UPDATE,true);
        order.verify(projects).lockActiveById(p);
    }
}
