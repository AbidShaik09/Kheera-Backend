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
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "TRUNCATE users, spaces CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProjectIntegrationTest extends PostgreSqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired SpaceLifecycleService spaces;
    @Autowired AuthenticationService auth;
    @Autowired ObjectMapper mapper;

    UUID space() throws Exception {
        if (users.findActiveByEmail("owner@example.com").isEmpty())
            users.saveAndFlush(new User("owner@example.com", "hash", "Owner"));
        return spaces.create("owner@example.com", mapper.readValue("{\"name\":\"Space\"}", SpaceWriteRequest.class)).id();
    }
    String token() { return "Bearer " + auth.generateToken("owner@example.com"); }
    String collection(UUID space) { return "/api/spaces/" + space + "/projects"; }
    String path(UUID project) { return "/api/projects/" + project; }
    UUID seed(UUID space, String name) {
        return jdbc.queryForObject("insert into projects(project_name,space_id) values (?,?) returning id", UUID.class,name,space);
    }
    @Test void lifecycleAndDescendantDeletion() throws Exception {
        UUID space=space();
        var created=mvc.perform(post(collection(space)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" Product \",\"description\":\"Details\",\"sprintCycleDays\":14}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Product"))
                .andExpect(jsonPath("$.spaceId").value(space.toString())).andExpect(jsonPath("$.sprintCycleDays").value(14))
                .andExpect(jsonPath("$.progressPercent").value(0)).andExpect(jsonPath("$.openTaskCount").value(0))
                .andReturn().getResponse();
        UUID project=UUID.fromString(mapper.readTree(created.getContentAsString()).get("id").asText());
        assertThat(created.getHeader("Location")).isEqualTo(path(project));
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk());
        mvc.perform(get(collection(space)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1)).andExpect(jsonPath("$.items[0].id").value(project.toString()));
        mvc.perform(patch(path(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\",\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.description").isEmpty()).andExpect(jsonPath("$.sprintCycleDays").value(14));
        UUID item=jdbc.queryForObject("insert into work_items(title,project_id) values ('Retained',?) returning id",UUID.class,project);
        mvc.perform(delete(path(project)).header("Authorization",token())).andExpect(status().isNoContent());
        for(String suffix:new String[]{"","/workflow-stages","/work-items"})
            mvc.perform(get(path(project)+suffix).header("Authorization",token())).andExpect(status().isNotFound());
        mvc.perform(post("/api/work-items/"+item+"/move").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isNotFound());
        mvc.perform(get(collection(space)).header("Authorization",token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
        assertThat(jdbc.queryForObject("select count(*) from work_items where id=? and not is_deleted",Integer.class,item)).isEqualTo(1);
    }
    @Test void authorizationAndInactiveAncestors() throws Exception {
        UUID space=space(), project=seed(space,"Hidden");
        mvc.perform(get(path(project))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get(collection(space))).andExpect(status().isUnauthorized());
        users.saveAndFlush(new User("foreign@example.com","hash","Foreign"));
        String foreign="Bearer "+auth.generateToken("foreign@example.com");
        mvc.perform(get(path(project)).header("Authorization",foreign)).andExpect(status().isNotFound());
        mvc.perform(get(collection(space)).header("Authorization",foreign)).andExpect(status().isNotFound());
        mvc.perform(patch(path(project)).header("Authorization",foreign).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijack\"}"))
                .andExpect(status().isNotFound());
        jdbc.update("update space_permissions set is_deleted=true where space_id=? and permission_name in ('space.update','space.delete')",space);
        mvc.perform(post(collection(space)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Denied\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(patch(path(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path(project)).header("Authorization",token())).andExpect(status().isForbidden());
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk());
        for(String table:new String[]{"space_members","space_roles","spaces"}) {
            jdbc.update("update "+table+" set is_deleted=true");
            mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isNotFound());
            mvc.perform(get(collection(space)).header("Authorization",token())).andExpect(status().isNotFound());
            jdbc.update("update "+table+" set is_deleted=false");
        }
        jdbc.update("update users set is_deleted=true");
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isUnauthorized());
    }
    @Test void validationAndPatchSemantics() throws Exception {
        UUID space=space(), project=seed(space,"Original");
        for(String body:new String[]{"{}","{\"name\":null}","{\"name\":\"  \"}","{\"name\":42}",
                "{\"name\":\""+"x".repeat(256)+"\"}","{\"name\":\"X\",\"description\":\""+"x".repeat(501)+"\"}",
                "{\"name\":\"X\",\"sprintCycleDays\":0}","{\"name\":\"X\",\"sprintCycleDays\":null}",
                "{\"name\":\"X\",\"sprintCycleDays\":1.5}","{\"name\":\"X\",\"sprintCycleDays\":2147483648}",
                "{\"name\":\"X\",\"spaceId\":\""+space+"\"}","{"}) {
            mvc.perform(post(collection(space)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(patch(path(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"spaceId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isBadRequest());
        var before=jdbc.queryForObject("select updated_at from projects where id=?",java.sql.Timestamp.class,project);
        mvc.perform(patch(path(project)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Original"));
        assertThat(jdbc.queryForObject("select updated_at from projects where id=?",java.sql.Timestamp.class,project)).isEqualTo(before);
        mvc.perform(get("/api/projects/bad-id").header("Authorization",token())).andExpect(status().isBadRequest());
        for(String query:new String[]{"page=-1","page=100001","size=0","size=101","sort=spaceId,asc","sort=name,up","q="+"x".repeat(101)})
            mvc.perform(get(collection(space)+"?"+query).header("Authorization",token())).andExpect(status().isBadRequest());
    }
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired ProjectService projectService;

    @Test void semanticMetricsIncludeParentsAndExcludeDeleted() throws Exception {
        UUID space=space(), project=seed(space,"Metrics"), empty=seed(space,"Empty");
        UUID complete=jdbc.queryForObject("select id from project_workflows where project_id=? and is_complete",UUID.class,project);
        UUID open=jdbc.queryForObject("select id from project_workflows where project_id=? and position=0",UUID.class,project);
        jdbc.update("update project_workflows set workflow_name='Shipped' where id=?",complete);
        jdbc.update("update project_workflows set workflow_name='Done' where id=?",open);
        UUID parent=jdbc.queryForObject("insert into work_items(title,project_id,workflow_id) values ('Epic',?,?) returning id",UUID.class,project,complete);
        jdbc.update("insert into work_items(title,project_id,workflow_id,parent_item_id,actual_end_date) values ('Child',?,?,?,now())",project,open,parent);
        UUID epicType=jdbc.queryForObject("insert into work_item_types(project_id,name) values (?,'Epic') returning id",UUID.class,project);
        jdbc.update("update work_items set work_item_type_id=? where id=?",epicType,parent);
        UUID other=jdbc.queryForObject("insert into work_items(title,project_id,workflow_id) values ('Open',?,?) returning id",UUID.class,project,open);
        jdbc.update("insert into work_items(title,project_id,workflow_id,is_deleted) values ('Deleted',?,?,true)",project,complete);
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(33)).andExpect(jsonPath("$.openTaskCount").value(2));
        mvc.perform(get(path(empty)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(0)).andExpect(jsonPath("$.openTaskCount").value(0));
        jdbc.update("update work_item_types set is_deleted=true where id=?",epicType);
        for(int i=0;i<5;i++) jdbc.update("insert into work_items(title,project_id,workflow_id) values ('Rounding',?,?)",project,open);
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(13)).andExpect(jsonPath("$.openTaskCount").value(7));
        jdbc.update("update work_items set is_deleted=true where project_id=? and title='Rounding'",project);
        jdbc.update("update work_items set is_deleted=true where id=?",other);
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(50)).andExpect(jsonPath("$.openTaskCount").value(1));
        jdbc.update("update project_workflows set is_complete=true where id=?",open);
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(100)).andExpect(jsonPath("$.openTaskCount").value(0));
        jdbc.update("update work_items set is_deleted=true where project_id=?",project);
        jdbc.update("update project_workflows set is_deleted=true where id=?",complete);
        mvc.perform(get(path(project)).header("Authorization",token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(0)).andExpect(jsonPath("$.openTaskCount").value(0));
    }

    @Test void scopedStablePagingAndBoundedQueries() throws Exception {
        UUID space=space(), foreignSpace=space();
        seed(foreignSpace,"Same");
        for(int i=0;i<16;i++) {
            UUID p=seed(space,"Same");
            jdbc.update("insert into work_items(title,project_id) values ('Open',?)",p);
        }
        UUID deleted=seed(space,"Same");
        jdbc.update("update projects set is_deleted=true where id=?",deleted);
        UUID literal=seed(space,"100%_literal");
        var expected=jdbc.queryForList("select id from projects where space_id=? and not is_deleted and project_name='Same' order by id",UUID.class,space);
        var first=projectService.list("owner@example.com",space,0,4,"name,asc","Same");
        var second=projectService.list("owner@example.com",space,1,4,"name,asc","Same");
        assertThat(first.totalItems()).isEqualTo(16);
        assertThat(first.items()).extracting(com.knightdevelopers.kheerabackend.dto.ProjectSummaryDto::id).containsExactlyElementsOf(expected.subList(0,4));
        assertThat(second.items()).extracting(com.knightdevelopers.kheerabackend.dto.ProjectSummaryDto::id).containsExactlyElementsOf(expected.subList(4,8));
        assertThat(first.items()).allSatisfy(p->{assertThat(p.openTaskCount()).isEqualTo(1);assertThat(p.progressPercent()).isZero();});
        assertThat(projectService.list("owner@example.com",space,0,25,"name,asc","%_").items())
                .extracting(com.knightdevelopers.kheerabackend.dto.ProjectSummaryDto::id).containsExactly(literal);
        assertThat(projectService.list("owner@example.com",space,0,25,"name,asc","sAmE").totalItems()).isEqualTo(16);
        var stats=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            projectService.list("owner@example.com",space,0,2,"name,asc","Same");
            long small=stats.getPrepareStatementCount();
            stats.clear();
            projectService.list("owner@example.com",space,0,10,"name,asc","Same");
            assertThat(stats.getPrepareStatementCount()).isEqualTo(small).isLessThanOrEqualTo(8);
            assertThat(stats.getCollectionFetchCount()).isZero();
        } finally { stats.setStatisticsEnabled(false); }
        mvc.perform(get(collection(space)).header("Authorization",token()).param("page","100000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.totalItems").value(17));
    }

    @Test void defaultCycleAndOpenApiMatchContract() throws Exception {
        UUID space=space();
        mvc.perform(post(collection(space)).header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Default\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sprintCycleDays").value(7));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/spaces/{spaceId}/projects'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}'].delete.responses['204']").exists());
    }

}
