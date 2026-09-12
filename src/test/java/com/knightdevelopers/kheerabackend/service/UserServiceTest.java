package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.LoginRequest;
import com.knightdevelopers.kheerabackend.dto.ResetPasswordRequest;
import com.knightdevelopers.kheerabackend.dto.UserResponse;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OTPVerificationService otpVerificationService = mock(OTPVerificationService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationService authenticationService = mock(AuthenticationService.class);
    private final UserService userService = new UserService(
            userRepository,
            otpVerificationService,
            passwordEncoder,
            authenticationService
    );

    @Test
    void authenticateLoginRequestUsesActiveUserLookup() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("member@example.com");
        request.setPassword("plain-password");
        User user = new User("member@example.com", "encoded-password", "Member");
        when(userRepository.findActiveByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(authenticationService.generateToken(user.getEmail())).thenReturn("jwt-token");

        String token = userService.authenticateLoginRequest(request);

        assertThat(token).isEqualTo("jwt-token");
        verify(userRepository).findActiveByEmail(request.getEmail());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void authenticateLoginRequestRejectsMissingOrSoftDeletedUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("deleted@example.com");
        request.setPassword("plain-password");
        when(userRepository.findActiveByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticateLoginRequest(request))
                .isInstanceOf(Exception.class)
                .hasMessage("Invalid Email Or Password");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(authenticationService, never()).generateToken(any());
    }

    @Test
    void resetUserPasswordUpdatesOnlyActiveUser() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("member@example.com");
        request.setPassword("new-password");
        request.setOtp(123456L);
        User user = new User("member@example.com", "old-password", "Member");
        when(otpVerificationService.validateOtp(any())).thenReturn(true);
        when(userRepository.findActiveByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("new-encoded-password");
        when(authenticationService.generateToken(user.getEmail())).thenReturn("jwt-token");

        String token = userService.resetUserPassword(request);

        assertThat(token).isEqualTo("jwt-token");
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).findActiveByEmail(request.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void getUsersReturnsRepositoryDtoProjection() {
        List<UserResponse> expectedUsers = List.of(
                new UserResponse(UUID.randomUUID(), "Member", "member@example.com")
        );
        when(userRepository.findActiveUserSummaries()).thenReturn(expectedUsers);

        List<UserResponse> users = userService.getUsers();

        assertThat(users).isEqualTo(expectedUsers);
        verify(userRepository).findActiveUserSummaries();
    }
}
