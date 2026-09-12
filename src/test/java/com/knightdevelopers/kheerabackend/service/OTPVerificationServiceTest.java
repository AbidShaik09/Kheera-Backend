package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.EmailRequest;
import com.knightdevelopers.kheerabackend.dto.OtpValidationRequest;
import com.knightdevelopers.kheerabackend.entity.Email;
import com.knightdevelopers.kheerabackend.entity.OneTimePassword;
import com.knightdevelopers.kheerabackend.repository.EmailRepository;
import com.knightdevelopers.kheerabackend.repository.OtpRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OTPVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-12T10:15:30Z");
    private final OtpRepository otpRepository = mock(OtpRepository.class);
    private final EmailRepository emailRepository = mock(EmailRepository.class);
    private final OTPVerificationService otpVerificationService = new OTPVerificationService(
            otpRepository,
            emailRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void generateOtpReturnsSixDigitValue() {
        for (int i = 0; i < 100; i++) {
            assertThat(OTPVerificationService.generateOtp()).isBetween(100000L, 999999L);
        }
    }

    @Test
    void sendSignUpOTPtoEmailSavesOtpAndQueuedEmail() {
        EmailRequest request = new EmailRequest();
        request.setEmail("member@example.com");

        otpVerificationService.sendSignUpOTPtoEmail(request);

        ArgumentCaptor<OneTimePassword> otpCaptor = ArgumentCaptor.forClass(OneTimePassword.class);
        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(otpRepository).save(otpCaptor.capture());
        verify(emailRepository).save(emailCaptor.capture());

        OneTimePassword otp = otpCaptor.getValue();
        Email email = emailCaptor.getValue();
        assertThat(otp.getEmail()).isEqualTo("member@example.com");
        assertThat(otp.getOtp()).isBetween(100000L, 999999L);
        assertThat(otp.getCreatedAt()).isEqualTo(NOW);
        assertThat(otp.getExpiresAt()).isEqualTo(Date.from(NOW.plusSeconds(15 * 60)));
        assertThat(email.getRecipientEmail()).isEqualTo("member@example.com");
        assertThat(email.getSubject()).isEqualTo("Kheera - Account Verification");
        assertThat(email.getBody()).contains(String.valueOf(otp.getOtp()));
        assertThat(email.getIsSent()).isFalse();
        assertThat(email.getIsUrgent()).isTrue();
    }

    @Test
    void sendPasswordResetOTPtoEmailSavesResetEmail() {
        otpVerificationService.sendPasswordResetOTPtoEmail("member@example.com");

        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(emailCaptor.capture());

        Email email = emailCaptor.getValue();
        assertThat(email.getRecipientEmail()).isEqualTo("member@example.com");
        assertThat(email.getSubject()).isEqualTo("Kheera - Password Reset");
        assertThat(email.getBody()).contains("change password");
    }

    @Test
    void validateOtpReturnsFalseWhenNoStoredOtpExists() {
        OtpValidationRequest request = validationRequest(123456L);
        when(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.empty());

        assertThat(otpVerificationService.validateOtp(request)).isFalse();
    }

    @Test
    void validateOtpReturnsFalseWhenLatestOtpDoesNotMatch() {
        OtpValidationRequest request = validationRequest(123456L);
        when(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(storedOtp(999999L, NOW.plusSeconds(60))));

        assertThat(otpVerificationService.validateOtp(request)).isFalse();
    }

    @Test
    void validateOtpAcceptsMatchingUnexpiredLatestOtp() {
        OtpValidationRequest request = validationRequest(123456L);
        when(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(storedOtp(123456L, NOW.plusSeconds(60))));

        assertThat(otpVerificationService.validateOtp(request)).isTrue();
    }

    @Test
    void validateOtpRejectsExpiredLatestOtp() {
        OtpValidationRequest request = validationRequest(123456L);
        when(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(storedOtp(123456L, NOW.minusSeconds(1))));

        assertThat(otpVerificationService.validateOtp(request)).isFalse();
    }

    @Test
    void validateOtpRejectsMissingExpiry() {
        OtpValidationRequest request = validationRequest(123456L);
        OneTimePassword storedOtp = storedOtp(123456L, NOW.plusSeconds(60));
        storedOtp.setExpiresAt(null);
        when(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(storedOtp));

        assertThat(otpVerificationService.validateOtp(request)).isFalse();
    }

    private static OtpValidationRequest validationRequest(Long otp) {
        OtpValidationRequest request = new OtpValidationRequest();
        request.setEmail("member@example.com");
        request.setOtp(otp);
        return request;
    }

    private static OneTimePassword storedOtp(Long otp, Instant expiresAt) {
        OneTimePassword oneTimePassword = new OneTimePassword();
        oneTimePassword.setEmail("member@example.com");
        oneTimePassword.setOtp(otp);
        oneTimePassword.setExpiresAt(Date.from(expiresAt));
        oneTimePassword.setCreatedAt(NOW.minusSeconds(10));
        return oneTimePassword;
    }
}
