package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.config.*;
import com.knightdevelopers.kheerabackend.dto.SpaceDetailDto;
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
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpaceLifecycleController.class)
@Import({SecurityConfig.class, CorsConfig.class, TimeConfig.class, JwtAuthenticationFilter.class, AuthenticationService.class})
@TestPropertySource(properties = "jwt.secret=abcdefghijklmnopqrstuvwxyz123456")
class SpaceLifecycleControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AuthenticationService auth;
    @MockBean SpaceLifecycleService service;
    final UUID id = UUID.randomUUID();
    String token() { return "Bearer " + auth.generateToken("owner@example.com"); }
    SpaceDetailDto detail() { return new SpaceDetailDto(id, "Product", null, null,
            Instant.EPOCH, Instant.EPOCH, new SpaceDetailDto.Capabilities(true, true, true)); }

    @Test void lifecycleUsesJwtEmailAndDocumentedStatuses() throws Exception {
        when(service.create(eq("owner@example.com"), any())).thenReturn(detail());
        when(service.detail("owner@example.com", id)).thenReturn(detail());
        when(service.update(eq("owner@example.com"), eq(id), any())).thenReturn(detail());
        mvc.perform(post("/api/spaces").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Product\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/spaces/" + id))
                .andExpect(jsonPath("$.name").value("Product")).andExpect(jsonPath("$.capabilities.canDelete").value(true));
        mvc.perform(get("/api/spaces/" + id).header("Authorization", token())).andExpect(status().isOk());
        mvc.perform(patch("/api/spaces/" + id).header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":null}")).andExpect(status().isOk());
        mvc.perform(delete("/api/spaces/" + id).header("Authorization", token())).andExpect(status().isNoContent());
        verify(service).delete("owner@example.com", id);
    }

    @Test void rejectsMissingAndInvalidJwtWithJson() throws Exception {
        mvc.perform(post("/api/spaces").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Product\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/spaces/" + id).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.fieldErrors").isMap());
        verifyNoInteractions(service);
    }

    @Test void rejectsForgedFieldsMalformedJsonAndInvalidUuid() throws Exception {
        for (String body : new String[]{"{\"ownerId\":\"forged\"}", "{\"name\":42}", "{"}) {
            mvc.perform(post("/api/spaces").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(get("/api/spaces/not-a-uuid").header("Authorization", token()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test void preservesNotFoundForbiddenAndValidationErrors() throws Exception {
        when(service.detail("owner@example.com", id)).thenThrow(SpaceApiException.notFound());
        mvc.perform(get("/api/spaces/" + id).header("Authorization", token()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        doThrow(SpaceApiException.forbidden()).when(service).delete("owner@example.com", id);
        mvc.perform(delete("/api/spaces/" + id).header("Authorization", token()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        when(service.create(eq("owner@example.com"), any())).thenThrow(SpaceApiException.invalid("name", "Name is required."));
        mvc.perform(post("/api/spaces").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").value("Name is required."));
    }
}
