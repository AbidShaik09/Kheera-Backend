package com.knightdevelopers.kheerabackend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.dto.SpaceWriteRequest;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "TRUNCATE users, spaces CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MembershipHttpIntegrationTest extends PostgreSqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired SpaceLifecycleService spaces;
    @Autowired AuthenticationService auth;
    @Autowired JdbcTemplate jdbc;

    @Test void journey() throws Exception {
        users.saveAndFlush(new User("owner@example.com", "hash", "Owner"));
        users.saveAndFlush(new User("member@example.com", "hash", "Member"));
        UUID space = spaces.create("owner@example.com", mapper.readValue("{\"name\":\"Team\"}", SpaceWriteRequest.class)).id();
        UUID role = jdbc.queryForObject("select id from space_roles where space_id=? and role_name='Administrator'", UUID.class, space);
        String path = "/api/spaces/" + space + "/members";
        String token = "Bearer " + auth.generateToken("owner@example.com");
        mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1)).andExpect(jsonPath("$.items[0].user.email").value("owner@example.com"));
        String body = "{\"email\":\"member@example.com\",\"roleId\":\"" + role + "\"}";
        String added = mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(added).get("id").asText();
        mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_MEMBERSHIP"));
        mvc.perform(patch(path + "/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleId\":\"" + role + "\"}")).andExpect(status().isOk());
        mvc.perform(delete(path + "/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get(path).header("Authorization", "Bearer " + auth.generateToken("member@example.com")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path + "?size=101").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/spaces/{spaceId}/members'].post.responses['201']").exists());
    }
}
