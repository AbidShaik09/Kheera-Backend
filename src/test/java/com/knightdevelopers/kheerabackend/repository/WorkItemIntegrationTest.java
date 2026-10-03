package com.knightdevelopers.kheerabackend.repository;

import com.fasterxml.jackson.databind.*;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.dto.SpaceWriteRequest;
import com.knightdevelopers.kheerabackend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.http.MediaType;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements="TRUNCATE users, spaces CASCADE", executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WorkItemIntegrationTest extends PostgreSqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired SpaceLifecycleService spaces;
    @Autowired AuthenticationService auth;
    @Autowired ObjectMapper mapper;
    UUID space() throws Exception {
        if(users.findActiveByEmail("owner@example.com").isEmpty()) users.saveAndFlush(new User("owner@example.com","hash","Owner"));
        return spaces.create("owner@example.com",mapper.readValue("{\"name\":\"Space\"}",SpaceWriteRequest.class)).id();
    }
    UUID project(UUID space) { return jdbc.queryForObject("insert into projects(project_name,space_id) values ('Project',?) returning id",UUID.class,space); }
    String token() { return "Bearer "+auth.generateToken("owner@example.com"); }
    String collection(UUID project) { return "/api/projects/"+project+"/work-items"; }
    String path(UUID item) { return "/api/work-items/"+item; }
    JsonNode create(UUID project,String body) throws Exception {
        var response=mvc.perform(post(collection(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse();
        var result=mapper.readTree(response.getContentAsString());
        assertThat(response.getHeader("Location")).isEqualTo(path(UUID.fromString(result.get("id").asText())));
        return result;
    }
    UUID id(JsonNode node) { return UUID.fromString(node.get("id").asText()); }
    @Test void lifecycleAndPatch() throws Exception {
        UUID space=space(), project=project(space);
        mvc.perform(get("/api/projects/"+project+"/work-item-types").header("Authorization",token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Task"));
        var created=create(project,"{\"title\":\" First \",\"description\":\"Keep\",\"efforts\":0,\"plannedStartDate\":\"2026-10-01T00:00:00Z\"}");
        UUID item=id(created);
        assertThat(created.get("title").asText()).isEqualTo("First");
        assertThat(created.get("stageName").asText()).isEqualTo("Backlog");
        assertThat(created.get("typeName").asText()).isEqualTo("Task");
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Keep"));
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null,\"plannedStartDate\":null,\"assigneeMemberId\":null,\"parentId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").isEmpty()).andExpect(jsonPath("$.plannedStartDate").isEmpty());
        mvc.perform(get(path(item)).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.efforts").value(0));
        mvc.perform(delete(path(item)).header("Authorization",token())).andExpect(status().isNoContent());
        mvc.perform(get(path(item)).header("Authorization",token())).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select is_deleted from work_items where id=?",Boolean.class,item)).isTrue();
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/work-items'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/work-items/{workItemId}'].delete.responses['204']").exists());
    }
    @Test void validationAndHierarchy() throws Exception {
        UUID project=project(space());
        for(String body:List.of("{}","{\"title\":null}","{\"title\":42}","{\"title\":\"  \"}",
                "{\"title\":\"X\",\"efforts\":-1}","{\"title\":\"X\",\"efforts\":1.2}",
                "{\"title\":\"X\",\"sprintId\":null}","{\"title\":\"X\",\"assigneeId\":null}",
                "{\"title\":\"X\",\"plannedStartDate\":\"tomorrow\"}",
                "{\"title\":\"X\",\"plannedStartDate\":\"2026-10-03T00:00:00Z\",\"plannedEndDate\":\"2026-10-01T00:00:00Z\"}")) {
            mvc.perform(post(collection(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        UUID parent=id(create(project,"{\"title\":\"Parent\"}"));
        UUID child=id(create(project,"{\"title\":\"Child\",\"parentId\":\""+parent+"\"}"));
        for(UUID target:List.of(parent,child))
            mvc.perform(patch(path(parent)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":\""+target+"\"}"))
                    .andExpect(status().isBadRequest());
        mvc.perform(delete(path(parent)).header("Authorization",token())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TASK_HAS_CHILDREN"));
        mvc.perform(patch(path(child)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":null}"))
                .andExpect(status().isOk());
        mvc.perform(delete(path(parent)).header("Authorization",token())).andExpect(status().isNoContent());
        mvc.perform(patch(path(child)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":\""+parent+"\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void authorizationAndRelationships() throws Exception {
        UUID space=space(), project=project(space), foreignProject=project(space());
        UUID item=id(create(project,"{\"title\":\"Private\"}"));
        UUID other=id(create(foreignProject,"{\"title\":\"Other\"}"));
        UUID foreignType=jdbc.queryForObject("select id from work_item_types where project_id=? limit 1",UUID.class,foreignProject);
        UUID foreignStage=jdbc.queryForObject("select id from project_workflows where project_id=? limit 1",UUID.class,foreignProject);
        UUID foreignMember=jdbc.queryForObject("select id from space_members where space_id=(select space_id from projects where id=?)",UUID.class,foreignProject);
        for(var entry:Map.of("parentId",other,"typeId",foreignType,"stageId",foreignStage,"assigneeMemberId",foreignMember).entrySet())
            mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(entry.getKey(),entry.getValue()))))
                    .andExpect(status().isBadRequest());
        mvc.perform(get(path(item))).andExpect(status().isUnauthorized());
        users.saveAndFlush(new User("outsider@example.com","hash","Outsider"));
        mvc.perform(get(path(item)).header("Authorization","Bearer "+auth.generateToken("outsider@example.com"))).andExpect(status().isNotFound());
        UUID member=jdbc.queryForObject("select id from space_members where space_id=?",UUID.class,space);
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"assigneeMemberId\":\""+member+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigneeName").value("Owner")).andExpect(jsonPath("$.assigneeActive").value(true));
        jdbc.update("update space_permissions set is_deleted=true where space_id=? and permission_name='space.update'",space);
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path(item)).header("Authorization",token())).andExpect(status().isForbidden());
        jdbc.update("update projects set is_deleted=true where id=?",project);
        mvc.perform(get(path(item)).header("Authorization",token())).andExpect(status().isNotFound());
    }
    @Test void filteredBoard() throws Exception {
        UUID space=space(), project=project(space);
        var parent=create(project,"{\"title\":\"Parent\"}");
        UUID member=jdbc.queryForObject("select id from space_members where space_id=?",UUID.class,space);
        UUID child=id(create(project,"{\"title\":\"100%_Literal\",\"parentId\":\""+id(parent)+"\",\"assigneeMemberId\":\""+member+"\"}"));
        create(project,"{\"title\":\"Unrelated\"}");
        mvc.perform(get(collection(project)).header("Authorization",token()).param("q","%_lIt").param("parentId",id(parent).toString())
                        .param("assigneeMemberId",member.toString()).param("typeId",parent.get("typeId").asText()).param("groupBy","stage").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1)).andExpect(jsonPath("$.items[0].id").value(child.toString()))
                .andExpect(jsonPath("$.items[0].assigneeName").value("Owner")).andExpect(jsonPath("$.groups[0].items[0].id").value(child.toString()));
        mvc.perform(get(collection(project)).header("Authorization",token()).param("size","2").param("page","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(3)).andExpect(jsonPath("$.items.length()").value(1));
        for(String query:List.of("sprintId="+UUID.randomUUID(),"assigneeId="+member,"page=100001","q="+"x".repeat(101)))
            mvc.perform(get(collection(project)+"?"+query).header("Authorization",token())).andExpect(status().isBadRequest());
        jdbc.update("update work_items set is_deleted=true where id=?",id(parent));
        mvc.perform(get(path(child)).header("Authorization",token())).andExpect(status().isNotFound());
        mvc.perform(post(path(child)+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+parent.get("stageId").asText()+"\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(collection(project)).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
    }
    @Test void concurrentHierarchyAndDeletion() throws Exception {
        UUID project=project(space()), a=id(create(project,"{\"title\":\"A\"}")), b=id(create(project,"{\"title\":\"B\"}"));
        var start=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var first=pool.submit(()->{start.await(); return mvc.perform(patch(path(a)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":\""+b+"\"}")).andReturn().getResponse().getStatus();});
            var second=pool.submit(()->{start.await(); return mvc.perform(patch(path(b)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":\""+a+"\"}")).andReturn().getResponse().getStatus();});
            start.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,400);
        }
    }

    @Test void historicalAssignmentAndInvalidPatchRollback() throws Exception {
        UUID space=space(), project=project(space);
        var user=users.saveAndFlush(new User("worker@example.com","hash","Worker"));
        UUID member=jdbc.queryForObject("insert into space_members(user_id,space_id,space_role_id) select ?,?,space_role_id from space_members where space_id=? limit 1 returning id",UUID.class,user.getId(),space,space);
        UUID item=id(create(project,"{\"title\":\"Assigned\",\"assigneeMemberId\":\""+member+"\",\"plannedStartDate\":\"2026-10-02T00:00:00Z\",\"plannedEndDate\":\"2026-10-03T00:00:00Z\"}"));
        jdbc.update("update space_members set is_deleted=true where id=?",member);
        mvc.perform(get(path(item)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName").value("Worker")).andExpect(jsonPath("$.assigneeActive").value(false));
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Do not persist\",\"plannedStartDate\":\"2026-10-04T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path(item)).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Assigned"));
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"assigneeMemberId\":\""+member+"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(path(item)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"assigneeMemberId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigneeMemberId").isEmpty());
    }

    @Test void createVersusParentDeletionNeverLeavesAnActiveOrphan() throws Exception {
        UUID project=project(space()), parent=id(create(project,"{\"title\":\"Parent\"}"));
        var start=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var creation=pool.submit(()->{start.await();return mvc.perform(post(collection(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Child\",\"parentId\":\""+parent+"\"}")).andReturn().getResponse().getStatus();});
            var deletion=pool.submit(()->{start.await();return mvc.perform(delete(path(parent)).header("Authorization",token())).andReturn().getResponse().getStatus();});
            start.countDown();
            int c=creation.get(20,TimeUnit.SECONDS), d=deletion.get(20,TimeUnit.SECONDS);
            assertThat((c==201 && d==409) || (c==400 && d==204)).isTrue();
        }
        assertThat(jdbc.queryForObject("select count(*) from work_items c join work_items p on p.id=c.parent_item_id where not c.is_deleted and p.is_deleted",Integer.class)).isZero();
    }

    @Test void stagePatchAndDeletionKeepBoardPositionsContiguous() throws Exception {
        UUID project=project(space());
        var first=create(project,"{\"title\":\"First\"}");
        var second=create(project,"{\"title\":\"Second\"}");
        var third=create(project,"{\"title\":\"Third\"}");
        UUID done=jdbc.queryForObject("select id from project_workflows where project_id=? and is_complete",UUID.class,project);
        mvc.perform(patch(path(id(first))).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"stageId\":\""+done+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.complete").value(true)).andExpect(jsonPath("$.position").value(0));
        assertThat(jdbc.queryForList("select position from work_items where workflow_id=? and not is_deleted order by position",Integer.class,UUID.fromString(second.get("stageId").asText())))
                .containsExactly(0,1);
        mvc.perform(delete(path(id(second))).header("Authorization",token())).andExpect(status().isNoContent());
        mvc.perform(get(path(id(third))).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.position").value(0));
        var fourth=create(project,"{\"title\":\"Fourth\"}");
        assertThat(fourth.get("position").asInt()).isEqualTo(1);
    }

    @Test void legacyForeignAncestorsAreHiddenFromBoardAndDetail() throws Exception {
        UUID project=project(space()), foreign=project(space());
        UUID parent=id(create(foreign,"{\"title\":\"Foreign\"}"));
        UUID child=id(create(project,"{\"title\":\"Bad legacy link\"}"));
        UUID grandchild=id(create(project,"{\"title\":\"Descendant\",\"parentId\":\""+child+"\"}"));
        jdbc.update("update work_items set parent_item_id=? where id=?",parent,child);
        for(boolean deleted:List.of(false,true)) {
            jdbc.update("update work_items set is_deleted=? where id=?",deleted,parent);
            mvc.perform(get(collection(project)).header("Authorization",token())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(0)).andExpect(jsonPath("$.items").isEmpty());
            for(UUID hidden:List.of(child,grandchild))
                mvc.perform(get(path(hidden)).header("Authorization",token())).andExpect(status().isNotFound());
        }
    }

    @Test void legacyForeignAssignmentAndTypeDoNotExposeOtherSpaceMetadata() throws Exception {
        UUID project=project(space()), foreignSpace=space(), foreign=project(foreignSpace);
        var user=users.saveAndFlush(new User("private@example.com","hash","Private foreign name"));
        UUID member=jdbc.queryForObject("insert into space_members(user_id,space_id,space_role_id) select ?,?,space_role_id from space_members where space_id=? limit 1 returning id",UUID.class,user.getId(),foreignSpace,foreignSpace);
        UUID type=jdbc.queryForObject("select id from work_item_types where project_id=?",UUID.class,foreign);
        jdbc.update("update work_item_types set name='Private foreign type' where id=?",type);
        UUID item=id(create(project,"{\"title\":\"Legacy\"}"));
        jdbc.update("update work_items set assigned_to_id=?,work_item_type_id=? where id=?",member,type,item);
        mvc.perform(get(path(item)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeActive").value(false)).andExpect(jsonPath("$.assigneeName").isEmpty())
                .andExpect(jsonPath("$.assigneeMemberId").isEmpty()).andExpect(jsonPath("$.typeId").isEmpty()).andExpect(jsonPath("$.typeName").isEmpty());
        mvc.perform(get(collection(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].assigneeName").isEmpty()).andExpect(jsonPath("$.items[0].typeName").isEmpty());
    }

    @Test void foreignLegacyChildrenCannotPermanentlyBlockDeletion() throws Exception {
        UUID project=project(space()), foreign=project(space());
        UUID parent=id(create(project,"{\"title\":\"Parent\"}"));
        UUID child=id(create(foreign,"{\"title\":\"Legacy child\"}"));
        jdbc.update("update work_items set parent_item_id=? where id=?",parent,child);
        mvc.perform(delete(path(parent)).header("Authorization",token())).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select is_deleted from work_items where id=?",Boolean.class,child)).isFalse();
    }

    @Test void hiddenTasksDoNotShiftBoardPositionsOrMoveIndexes() throws Exception {
        UUID project=project(space()), foreign=project(space());
        UUID parent=id(create(foreign,"{\"title\":\"Foreign parent\"}"));
        var hidden=create(project,"{\"title\":\"Hidden\"}");
        var a=create(project,"{\"title\":\"A\"}");
        var b=create(project,"{\"title\":\"B\"}");
        jdbc.update("update work_items set parent_item_id=? where id=?",parent,id(hidden));
        mvc.perform(get(collection(project)).header("Authorization",token()).param("q","B"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].position").value(1));
        mvc.perform(get(path(id(b))).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.position").value(1));
        mvc.perform(post(path(id(b))+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\""+a.get("stageId").asText()+"\",\"position\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(1));
        mvc.perform(get(collection(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id(a).toString())).andExpect(jsonPath("$.items[1].id").value(id(b).toString()))
                .andExpect(jsonPath("$.items[0].position").value(0)).andExpect(jsonPath("$.items[1].position").value(1));
        var c=create(project,"{\"title\":\"C\"}");
        assertThat(c.get("position").asInt()).isEqualTo(2);
        UUID target=jdbc.queryForObject("select id from project_workflows where project_id=? and position=1",UUID.class,project);
        var targetHidden=create(project,"{\"title\":\"Hidden target\",\"stageId\":\""+target+"\"}");
        var d=create(project,"{\"title\":\"D\",\"stageId\":\""+target+"\"}");
        jdbc.update("update work_items set parent_item_id=? where id=?",parent,id(targetHidden));
        mvc.perform(post(path(id(b))+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\""+target+"\",\"position\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(1));
        mvc.perform(get(collection(project)).header("Authorization",token()).param("stageId",target.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(id(d).toString()))
                .andExpect(jsonPath("$.items[1].id").value(id(b).toString()));
        mvc.perform(get(path(id(c))).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.position").value(1));
    }
}
