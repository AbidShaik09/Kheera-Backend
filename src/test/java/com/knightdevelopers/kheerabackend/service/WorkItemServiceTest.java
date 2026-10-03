package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.WorkItemWriteRequest;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkItemServiceTest {
    ProjectsRepository projects=mock(ProjectsRepository.class);
    WorkItemsRepository items=mock(WorkItemsRepository.class);
    WorkItemTypesRepository types=mock(WorkItemTypesRepository.class);
    WorkflowStageRepository stages=mock(WorkflowStageRepository.class);
    SpaceMembersRepository members=mock(SpaceMembersRepository.class);
    SpaceAccessService access=mock(SpaceAccessService.class);
    WorkItemService service=new WorkItemService(projects,items,types,stages,members,access);
    @Test void deniedCreateNeverReadsTaskDataOrLocksProject() {
        UUID project=UUID.randomUUID(),space=UUID.randomUUID();
        when(projects.findActiveSpaceId(project)).thenReturn(Optional.of(space));
        doThrow(SpaceApiException.forbidden()).when(access).requireSpace("caller",space,SpaceAccessService.UPDATE,true);
        assertThatThrownBy(()->service.create("caller",project,new WorkItemWriteRequest())).isInstanceOf(SpaceApiException.class);
        verify(projects,never()).lockActiveById(any());
        verifyNoInteractions(items,types,stages,members);
    }
    @Test void locksSpaceThenProjectBeforeReadingMutableTask() {
        UUID project=UUID.randomUUID(),space=UUID.randomUUID(),item=UUID.randomUUID();
        when(items.findActiveProjectId(item)).thenReturn(Optional.of(project));
        when(projects.findActiveSpaceId(project)).thenReturn(Optional.of(space));
        when(projects.lockActiveById(project)).thenReturn(Optional.of(new Projects()));
        assertThatThrownBy(()->service.delete("caller",item)).isInstanceOf(SpaceApiException.class);
        var order=inOrder(access,projects,items);
        order.verify(access).requireSpace("caller",space,SpaceAccessService.UPDATE,true);
        order.verify(projects).lockActiveById(project);
        order.verify(items).isVisible(item);
    }
}
