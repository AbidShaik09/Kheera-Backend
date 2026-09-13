package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.config.*;
import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.security.JwtAuthenticationFilter;
import com.knightdevelopers.kheerabackend.service.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MembershipController.class)
@Import({SecurityConfig.class, CorsConfig.class, TimeConfig.class, JwtAuthenticationFilter.class, AuthenticationService.class})
@TestPropertySource(properties = "jwt.secret=abcdefghijklmnopqrstuvwxyz123456")
class MembershipControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AuthenticationService auth;
    @MockBean MembershipService service;
    final UUID space = UUID.randomUUID(), member = UUID.randomUUID(), role = UUID.randomUUID();
    final String path = "/api/spaces/" + space + "/members";
    String token() { return "Bearer " + auth.generateToken("actor@example.com"); }
    @Test void statuses() throws Exception {
        var dto = new MembershipDto(member, new MembershipDto.UserSummary(UUID.randomUUID(), "Name", "member@example.com"), new MembershipDto.RoleDto(role, "Member"));
        when(service.add(eq("actor@example.com"), eq(space), any())).thenReturn(dto);
        when(service.changeRole(eq("actor@example.com"), eq(space), eq(member), any())).thenReturn(dto);
        mvc.perform(post(path).header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"member@example.com\",\"roleId\":\"" + role + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(member.toString()));
        mvc.perform(patch(path + "/" + member).header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleId\":\"" + role + "\"}")).andExpect(status().isOk());
        mvc.perform(delete(path + "/" + member).header("Authorization", token())).andExpect(status().isNoContent());
        verify(service).remove("actor@example.com", space, member);
    }
    @Test void invalidRequests() throws Exception {
        for (String body : List.of("{", "{\"roleId\":12}", "{\"roleId\":null}", "{\"roleId\":\"1-1-1-1-1\"}", "{\"userId\":\"forged\"}")) {
            mvc.perform(post(path).header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(patch(path + "/" + member).header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"forged@example.com\"}")).andExpect(status().isBadRequest());
        mvc.perform(get(path + "?size=abc").header("Authorization", token())).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void authenticationAndErrors() throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get(path).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        doThrow(SpaceApiException.forbidden()).when(service).remove("actor@example.com", space, member);
        mvc.perform(delete(path + "/" + member).header("Authorization", token())).andExpect(status().isForbidden());
        doThrow(SpaceApiException.conflict("LAST_ADMINISTRATOR", "At least one active administrator must remain.")).when(service).remove("actor@example.com", space, member);
        mvc.perform(delete(path + "/" + member).header("Authorization", token())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMINISTRATOR"));
    }
}
