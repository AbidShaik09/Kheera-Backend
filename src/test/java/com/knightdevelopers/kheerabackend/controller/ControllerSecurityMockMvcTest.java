package com.knightdevelopers.kheerabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.config.CorsConfig;
import com.knightdevelopers.kheerabackend.config.SecurityConfig;
import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.security.JwtAuthenticationFilter;
import com.knightdevelopers.kheerabackend.service.AuthenticationService;
import com.knightdevelopers.kheerabackend.service.OTPVerificationService;
import com.knightdevelopers.kheerabackend.service.SpaceService;
import com.knightdevelopers.kheerabackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AuthenticationController.class,
        UserController.class,
        SpaceController.class,
        HealthController.class
})
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        JwtAuthenticationFilter.class,
        AuthenticationService.class
})
@TestPropertySource(properties = "jwt.secret=abcdefghijklmnopqrstuvwxyz123456")
class ControllerSecurityMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private OTPVerificationService otpVerificationService;

    @MockBean
    private SpaceService spaceService;

    @Test
    void loginReturnsRawTokenText() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("member@example.com");
        request.setPassword("secret");
        when(userService.authenticateLoginRequest(any(LoginRequest.class))).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));
    }

    @Test
    void loginFailureReturnsRawInvalidCredentialsText() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("member@example.com");
        request.setPassword("wrong");
        when(userService.authenticateLoginRequest(any(LoginRequest.class))).thenThrow(new RuntimeException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid Email Or Password"));
    }

    @Test
    void signupEmailReturnsRawOtpSentText() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmail("new@example.com");
        when(userService.isAnExistingUser("new@example.com")).thenReturn(false);

        mockMvc.perform(post("/api/auth/signup-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP has Been Sent To Your Email"));

        verify(otpVerificationService).sendSignUpOTPtoEmail(any(EmailRequest.class));
    }

    @Test
    void signupEmailRejectsExistingUserWithRawText() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmail("existing@example.com");
        when(userService.isAnExistingUser("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/signup-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already registered!"));

        verify(otpVerificationService, never()).sendSignUpOTPtoEmail(any(EmailRequest.class));
    }

    @Test
    void otpValidationReturnsRawSuccessAndFailureText() throws Exception {
        OtpValidationRequest valid = new OtpValidationRequest();
        valid.setEmail("member@example.com");
        valid.setOtp(123456L);
        when(otpVerificationService.validateOtp(any(OtpValidationRequest.class))).thenReturn(true, false);

        mockMvc.perform(post("/api/auth/otp-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valid)))
                .andExpect(status().isOk())
                .andExpect(content().string("Valid OTP, Proceed"));

        mockMvc.perform(post("/api/auth/otp-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Or Expired OTP"));
    }

    @Test
    void signupAndResetPasswordReturnRawTokenText() throws Exception {
        SignUpRequest signup = new SignUpRequest();
        signup.setEmail("new@example.com");
        signup.setPassword("secret");
        signup.setName("New User");
        signup.setOtp(123456L);
        when(userService.isAnExistingUser("new@example.com")).thenReturn(false);
        when(userService.createUser(any(SignUpRequest.class))).thenReturn("signup-token");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk())
                .andExpect(content().string("signup-token"));

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setEmail("new@example.com");
        reset.setPassword("changed");
        reset.setOtp(123456L);
        when(userService.resetUserPassword(any(ResetPasswordRequest.class))).thenReturn("reset-token");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isOk())
                .andExpect(content().string("reset-token"));
    }

    @Test
    void forgotPasswordAlwaysReturnsRawOtpSentTextAndOnlySendsForKnownUser() throws Exception {
        EmailRequest known = new EmailRequest();
        known.setEmail("known@example.com");
        when(userService.isAnExistingUser("known@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(known)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP has Been Sent To Your Email"));

        verify(otpVerificationService).sendPasswordResetOTPtoEmail("known@example.com");
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Server is Healthy"));
    }

    @Test
    void userListDoesNotExposePasswords() throws Exception {
        UserResponse member = new UserResponse(UUID.randomUUID(), "Member", "member@example.com");
        when(userService.getUsers()).thenReturn(List.of(member));
        String token = authenticationService.generateToken("member@example.com");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Member"))
                .andExpect(jsonPath("$[0].email").value("member@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void currentUserUsesJwtEmailPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getCurrentUser("member@example.com"))
                .thenReturn(Optional.of(new UserResponse(userId, "Member", "member@example.com")));
        String token = authenticationService.generateToken("member@example.com");

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void spaceListUsesJwtEmailPrincipalWithoutParsingUuid() throws Exception {
        UUID spaceId = UUID.randomUUID();
        when(spaceService.getSpacesForUserEmail("member@example.com"))
                .thenReturn(List.of(new SpaceListDto(spaceId, "Workspace")));
        String token = authenticationService.generateToken("member@example.com");

        mockMvc.perform(get("/api/spaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(spaceId.toString()))
                .andExpect(jsonPath("$[0].name").value("Workspace"));

        verify(spaceService).getSpacesForUserEmail(eq("member@example.com"));
    }

    @Test
    void missingMalformedAndInvalidBearerTokensCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/spaces"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/spaces").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/spaces").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isForbidden());

        verify(spaceService, never()).getSpacesForUserEmail(any());
    }

    @Test
    void corsPreflightAllowsConfiguredOriginHeadersAndMethods() throws Exception {
        mockMvc.perform(options("/api/spaces")
                        .header(HttpHeaders.ORIGIN, "https://kheera.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://kheera.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("GET")));
    }
}
