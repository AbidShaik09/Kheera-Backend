package com.knightdevelopers.kheerabackend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.dto.SpaceWriteRequest;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.service.SpaceLifecycleService;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.knightdevelopers.kheerabackend.service.AuthenticationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "TRUNCATE users, spaces CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SpaceLifecycleIntegrationTest extends PostgreSqlIntegrationTest {
    @Autowired SpaceLifecycleService service;
    @Autowired UserRepository users;
    @Autowired SpaceMembersRepository members;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;
    @Autowired AuthenticationService auth;

    SpaceWriteRequest input(String json) throws Exception { return mapper.readValue(json, SpaceWriteRequest.class); }
    void owner() { users.saveAndFlush(new User("owner@example.com", "hash", "Owner")); }

    @Test void openApiDocumentsActualStatusesAndFieldTypes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/spaces'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/spaces/{spaceId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.components.schemas.SpaceWriteRequest.properties.name.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.SpaceWriteRequest.properties.name.maxLength").value(255));
    }

    @Test void authenticatedHttpJourneyPersistsAndEnforcesIsolation() throws Exception {
        owner(); users.saveAndFlush(new User("other@example.com", "hash", "Other"));
        String token = "Bearer " + auth.generateToken("owner@example.com");
        String other = "Bearer " + auth.generateToken("other@example.com");
        String response = mvc.perform(post("/api/spaces").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"HTTP space\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(response).get("id").asText();
        mvc.perform(get("/api/spaces").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/spaces/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/spaces/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":null}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/spaces/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"));
        mvc.perform(delete("/api/spaces/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/spaces/" + id).header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void committedBootstrapIsVisibleAndSoftDeleteRemovesAccess() throws Exception {
        owner();
        var created = service.create("owner@example.com", input("{\"name\":\"Space\"}"));
        assertThat(members.findActiveSpacesByUserEmail("owner@example.com")).hasSize(1);
        assertThat(service.detail("owner@example.com", created.id()).capabilities().canManageMembers()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from space_role_permissions", Integer.class)).isEqualTo(8);
        jdbc.update("insert into projects(project_name, space_id) values ('Retained project', ?)", created.id());
        service.delete("owner@example.com", created.id());
        assertThat(members.findActiveSpacesByUserEmail("owner@example.com")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from spaces", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from space_members", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from projects where is_deleted=false", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> service.detail("owner@example.com", created.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
    }

    @Test void excludesDeletedUsersMembershipsRolesAndPermissions() throws Exception {
        owner();
        var created = service.create("owner@example.com", input("{\"name\":\"Space\"}"));
        jdbc.update("update space_permissions set is_deleted=true where permission_name='space.delete'");
        assertThat(service.detail("owner@example.com", created.id()).capabilities().canDelete()).isFalse();
        assertThatThrownBy(() -> service.delete("owner@example.com", created.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(403));
        jdbc.update("update space_roles set is_deleted=true");
        assertThatThrownBy(() -> service.detail("owner@example.com", created.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
        jdbc.update("update space_roles set is_deleted=false");
        jdbc.update("update space_members set is_deleted=true");
        assertThatThrownBy(() -> service.detail("owner@example.com", created.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
        jdbc.update("update users set is_deleted=true");
        assertThatThrownBy(() -> service.detail("owner@example.com", created.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(401));
    }

    @Test void patchPersistsOnlyRequestedFieldsAndRejectsNullName() throws Exception {
        owner();
        var created = service.create("owner@example.com", input("{\"name\":\"First\",\"description\":\"Keep\",\"profilePic\":\"https://example.com/image.png\"}"));
        service.update("owner@example.com", created.id(), input("{\"name\":\" Second \",\"profilePic\":null}"));
        var detail = service.detail("owner@example.com", created.id());
        assertThat(detail.name()).isEqualTo("Second");
        assertThat(detail.description()).isEqualTo("Keep");
        assertThat(detail.profilePic()).isNull();
        var invalid = input("{\"name\":null,\"description\":\"Do not save\"}");
        assertThatThrownBy(() -> service.update("owner@example.com", created.id(), invalid)).isInstanceOf(SpaceApiException.class);
        assertThat(service.detail("owner@example.com", created.id()).description()).isEqualTo("Keep");
    }

    @Test void failedMembershipInsertRollsBackEntireBootstrap() throws Exception {
        owner();
        jdbc.execute("ALTER TABLE space_members ADD CONSTRAINT issue66_reject_insert CHECK (is_deleted = true)");
        try {
            var request = input("{\"name\":\"Rollback\"}");
            assertThatThrownBy(() -> service.create("owner@example.com", request)).isInstanceOf(RuntimeException.class);
            for (String table : new String[]{"spaces", "space_roles", "space_permissions", "space_role_permissions", "space_members"}) {
                assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class)).isZero();
            }
        } finally { jdbc.execute("ALTER TABLE space_members DROP CONSTRAINT issue66_reject_insert"); }
    }

    @Test void isolatesUsersAndRejectsRevokedOrCrossSpaceGrants() throws Exception {
        owner(); users.saveAndFlush(new User("other@example.com", "hash", "Other"));
        var first = service.create("owner@example.com", input("{\"name\":\"First\"}"));
        var second = service.create("other@example.com", input("{\"name\":\"Second\"}"));
        assertThatThrownBy(() -> service.detail("other@example.com", first.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
        jdbc.update("""
                update space_role_permissions set space_permission_id=(select id from space_permissions where space_id=? and permission_name='space.update')
                where space_role_id in (select id from space_roles where space_id=?)
                and space_permission_id in (select id from space_permissions where space_id=? and permission_name='space.update')
                """, second.id(), first.id(), first.id());
        assertThat(service.detail("owner@example.com", first.id()).capabilities().canUpdate()).isFalse();
        jdbc.update("update space_role_permissions set is_deleted=true where space_role_id in (select id from space_roles where space_id=?)", first.id());
        assertThat(service.detail("owner@example.com", first.id()).capabilities().canUpdate()).isFalse();
        var patch = input("{\"name\":\"Forged\"}");
        assertThatThrownBy(() -> service.update("owner@example.com", first.id(), patch))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(403));
        jdbc.update("update space_members set space_role_id=(select id from space_roles where space_id=? and role_name='Administrator') where space_id=?", second.id(), first.id());
        assertThatThrownBy(() -> service.detail("owner@example.com", first.id()))
                .isInstanceOfSatisfying(SpaceApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(404));
    }
}
