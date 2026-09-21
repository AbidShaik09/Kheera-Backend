package com.knightdevelopers.kheerabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.dto.ProjectWriteRequest;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
import com.knightdevelopers.kheerabackend.repository.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectServiceTest {
    final ProjectsRepository projects=mock(ProjectsRepository.class);
    final WorkItemsRepository items=mock(WorkItemsRepository.class);
    final SpaceAccessService access=mock(SpaceAccessService.class);
    final ProjectService service=new ProjectService(projects,items,access);
    final UUID space=UUID.randomUUID(), project=UUID.randomUUID();
    @Test void deniedAccessDoesNotLoadSummaries() {
        when(access.requireSpace("user",space,null,false)).thenThrow(SpaceApiException.notFound());
        assertThatThrownBy(()->service.list("user",space,0,25,"name,asc","")).isInstanceOf(SpaceApiException.class);
        verifyNoInteractions(projects,items);
    }
    @Test void deletionLocksSpaceThenProjectAndRetainsChildren() {
        var entity=new Projects();
        when(projects.findActiveSpaceId(project)).thenReturn(Optional.of(space));
        when(projects.lockActiveById(project)).thenReturn(Optional.of(entity));
        service.delete("user",project);
        var order=inOrder(access,projects);
        order.verify(access).requireActiveUser("user");
        order.verify(projects).findActiveSpaceId(project);
        order.verify(access).requireSpace("user",space,SpaceAccessService.DELETE,true);
        order.verify(projects).lockActiveById(project);
        assertThat(entity.isDeleted()).isTrue();
        verify(projects,never()).delete(any());
        verifyNoInteractions(items);
    }
    @Test void inactiveAccountsCannotDiscoverProjectExistence() {
        doThrow(SpaceApiException.unauthorized()).when(access).requireActiveUser("inactive");
        assertThatThrownBy(()->service.detail("inactive",project)).isInstanceOf(SpaceApiException.class);
        assertThatThrownBy(()->service.update("inactive",project,new ProjectWriteRequest())).isInstanceOf(SpaceApiException.class);
        assertThatThrownBy(()->service.delete("inactive",project)).isInstanceOf(SpaceApiException.class);
        verifyNoInteractions(projects,items);
    }
    @Test void deniedMutationCannotLockOrWriteProject() throws Exception {
        when(projects.findActiveSpaceId(project)).thenReturn(Optional.of(space));
        when(access.requireSpace("user",space,SpaceAccessService.UPDATE,true)).thenThrow(SpaceApiException.forbidden());
        var request=new ObjectMapper().readValue("{\"name\":\"Changed\"}",ProjectWriteRequest.class);
        assertThatThrownBy(()->service.update("user",project,request)).isInstanceOf(SpaceApiException.class);
        verify(projects,never()).lockActiveById(any());
        verifyNoInteractions(items);
    }
}
