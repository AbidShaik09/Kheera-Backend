package com.knightdevelopers.kheerabackend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.service.*;
import com.knightdevelopers.kheerabackend.dto.SpaceWriteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "TRUNCATE users, spaces CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WorkflowBoardIntegrationTest extends PostgreSqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired SpaceLifecycleService spaces;
    @Autowired AuthenticationService auth;
    @Autowired ObjectMapper mapper;

    UUID project() throws Exception {
        if (users.findActiveByEmail("owner@example.com").isEmpty()) users.saveAndFlush(new User("owner@example.com", "hash", "Owner"));
        UUID space = spaces.create("owner@example.com", mapper.readValue("{\"name\":\"Board\"}", SpaceWriteRequest.class)).id();
        return jdbc.queryForObject("insert into projects(project_name,space_id) values ('Board',?) returning id", UUID.class, space);
    }
    String token() { return "Bearer " + auth.generateToken("owner@example.com"); }
    String stages(UUID project) { return "/api/projects/" + project + "/workflow-stages"; }
    UUID firstStage(UUID project) throws Exception {
        String json = mvc.perform(get(stages(project)).header("Authorization",token())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(json).get(0).get("id").asText());
    }
    UUID item(UUID project, UUID stage, int position) {
        return jdbc.queryForObject("insert into work_items(title,project_id,workflow_id,position) values ('Task',?,?,?) returning id", UUID.class,project,stage,position);
    }
    @Test void freshProjectsHaveSixOrderedStagesAndSemanticCompletion() throws Exception {
        UUID project = project();
        mvc.perform(get(stages(project)).header("Authorization", token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("Backlog"))
                .andExpect(jsonPath("$[4].complete").value(true));
    }
    @Test void moveReordersItemsAndFilteredBoardReturnsTheirPositions() throws Exception {
        UUID project = project(), source = firstStage(project);
        UUID target = jdbc.queryForObject("select id from project_workflows where project_id=? and position=1",UUID.class,project);
        UUID one=item(project,source,0), two=item(project,source,1), three=item(project,target,0);
        mvc.perform(post("/api/work-items/"+one+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+target+"\",\"position\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(one.toString()))
                .andExpect(jsonPath("$.position").value(0));
        assertThat(jdbc.queryForObject("select position from work_items where id=?",Integer.class,two)).isZero();
        assertThat(jdbc.queryForObject("select position from work_items where id=?",Integer.class,three)).isEqualTo(1);
        mvc.perform(get("/api/projects/"+project+"/work-items").param("stageId",target.toString()).header("Authorization",token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].id").value(one.toString()));
    }
    @Test void schemaRejectsForeignProjectStage() throws Exception {
        UUID first=project(), second=project(), stage=firstStage(first);
        assertThatThrownBy(()->item(second,stage,0)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void missingJwtHasContractError() throws Exception {
        mvc.perform(get(stages(UUID.randomUUID()))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test void stageAdministrationReordersWithoutUniqueCollisionsAndProtectsNonemptyAndLastStage() throws Exception {
        UUID p=project(), backlog=firstStage(p);
        String response=mvc.perform(post(stages(p)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" Custom \",\"position\":0,\"complete\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Custom")).andReturn().getResponse().getContentAsString();
        UUID custom=UUID.fromString(mapper.readTree(response).get("id").asText());
        mvc.perform(patch(stages(p)+"/"+custom).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\",\"position\":999}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true)).andExpect(jsonPath("$.position").value(6));
        item(p,custom,0);
        mvc.perform(delete(stages(p)+"/"+custom).header("Authorization",token())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAGE_NOT_EMPTY"));
        for(UUID id:jdbc.queryForList("select id from project_workflows where project_id=? and id<>?",UUID.class,p,custom))
            mvc.perform(delete(stages(p)+"/"+id).header("Authorization",token())).andExpect(status().isNoContent());
        mvc.perform(delete(stages(p)+"/"+custom).header("Authorization",token())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_STAGE"));
        assertThat(jdbc.queryForObject("select position from project_workflows where id=?",Integer.class,custom)).isZero();
    }

    @Test void sameStageMovesAppendClampAndRepeatDeterministically() throws Exception {
        UUID p=project(), stage=firstStage(p);
        UUID a=item(p,stage,0), b=item(p,stage,1), c=item(p,stage,2);
        for (String body : new String[]{"{\"stageId\":\""+stage+"\"}", "{\"stageId\":\""+stage+"\",\"position\":999}"}) {
            mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(2));
            assertThat(jdbc.queryForList("select id from work_items where workflow_id=? order by position",UUID.class,stage)).containsExactly(b,c,a);
        }
    }

    @Test void rejectsForeignDeletedAndUnauthorizedTargetsWithoutChangingOrder() throws Exception {
        UUID p=project(), source=firstStage(p), other=project(), foreign=firstStage(other), a=item(p,source,0);
        mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+foreign+"\"}")).andExpect(status().isNotFound());
        users.saveAndFlush(new User("outsider@example.com","hash","Outsider"));
        mvc.perform(get(stages(p)).header("Authorization","Bearer "+auth.generateToken("outsider@example.com"))).andExpect(status().isNotFound());
        jdbc.update("update space_permissions set is_deleted=true where permission_name='space.update'");
        mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+source+"\"}")).andExpect(status().isForbidden());
        jdbc.update("update space_permissions set is_deleted=false where permission_name='space.update'");
        jdbc.update("update projects set is_deleted=true where id=?",p);
        mvc.perform(get(stages(p)).header("Authorization",token())).andExpect(status().isNotFound());
        mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+source+"\"}")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select workflow_id from work_items where id=?",UUID.class,a)).isEqualTo(source);
    }

    @Test void excludesDeletedMembershipUserRoleSpaceAndWorkItem() throws Exception {
        UUID p=project(), stage=firstStage(p), a=item(p,stage,0);
        for(String table:new String[]{"space_members","space_roles","spaces"}) {
            jdbc.update("update "+table+" set is_deleted=true");
            mvc.perform(get(stages(p)).header("Authorization",token())).andExpect(status().isNotFound());
            jdbc.update("update "+table+" set is_deleted=false");
        }
        jdbc.update("update users set is_deleted=true");
        mvc.perform(get(stages(p)).header("Authorization",token())).andExpect(status().isUnauthorized());
        jdbc.update("update users set is_deleted=false");
        jdbc.update("update work_items set is_deleted=true where id=?",a);
        mvc.perform(get("/api/projects/"+p+"/work-items").header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+stage+"\"}")).andExpect(status().isNotFound());
    }

    @Test void groupedPaginationIncludesEmptyColumnsAndOnlyCurrentPage() throws Exception {
        UUID p=project(), stage=firstStage(p);
        item(p,stage,0); item(p,stage,1);
        mvc.perform(get("/api/projects/"+p+"/work-items").header("Authorization",token()).param("groupBy","stage").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.groups.length()").value(6))
                .andExpect(jsonPath("$.groups[0].items.length()").value(1)).andExpect(jsonPath("$.groups[1].items").isEmpty());
        mvc.perform(get("/api/projects/"+p+"/work-items").header("Authorization",token()).param("size","101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/projects/"+p+"/work-items").header("Authorization",token()).param("groupBy","invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/projects/"+p+"/work-items").header("Authorization",token()).param("stageId",UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test void concurrentMovesPreserveContiguousUniquePositions() throws Exception {
        UUID p=project(), stage=firstStage(p), a=item(p,stage,0), b=item(p,stage,1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate=new java.util.concurrent.CountDownLatch(1);
            var futures=new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for(UUID id:new UUID[]{a,b}) futures.add(executor.submit(()->{
                gate.await();
                mvc.perform(post("/api/work-items/"+id+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\""+stage+"\",\"position\":0}")).andExpect(status().isOk());
                return null;
            }));
            gate.countDown();
            for(var future:futures) future.get(20,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForList("select position from work_items where workflow_id=? order by position",Integer.class,stage)).containsExactly(0,1);
    }

    @Test void concurrentStageDeletionAndMoveCannotLeaveAnActiveItemInADeletedStage() throws Exception {
        UUID p=project(), source=firstStage(p), a=item(p,source,0);
        UUID target=jdbc.queryForObject("select id from project_workflows where project_id=? and position=1",UUID.class,p);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate=new java.util.concurrent.CountDownLatch(1);
            var move=executor.submit(()->{
                gate.await();
                return mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stageId\":\""+target+"\"}"))
                        .andReturn().getResponse().getStatus();
            });
            var deletion=executor.submit(()->{
                gate.await();
                return mvc.perform(delete(stages(p)+"/"+target).header("Authorization",token()))
                        .andReturn().getResponse().getStatus();
            });
            gate.countDown();
            int moveStatus=move.get(20,java.util.concurrent.TimeUnit.SECONDS);
            int deleteStatus=deletion.get(20,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(moveStatus==200 && deleteStatus==409 || moveStatus==404 && deleteStatus==204).isTrue();
        }
        assertThat(jdbc.queryForObject("select count(*) from work_items w join project_workflows s on s.id=w.workflow_id where w.id=? and not w.is_deleted and s.is_deleted",Integer.class,a)).isZero();
    }

    @Test void strictInputAndOpenApiExposeContract() throws Exception {
        UUID p=project(), stage=firstStage(p), a=item(p,stage,0);
        for(String body:new String[]{"{}", "{\"name\":null}", "{\"name\":42}", "{\"name\":\"   \"}", "{\"name\":\"A\",\"position\":1.2}", "{\"name\":\"A\",\"complete\":\"true\"}", "{\"name\":\"A\",\"ownerId\":\"forged\"}"})
            mvc.perform(post(stages(p)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        for(String body:new String[]{"{}", "{\"stageId\":null}", "{\"stageId\":\"bad\"}", "{\"stageId\":\""+stage+"\",\"position\":-1}", "{\"stageId\":\""+stage+"\",\"position\":\"1\"}"})
            mvc.perform(post("/api/work-items/"+a+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/workflow-stages'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/work-items/{workItemId}/move'].post").exists());
    }
}
