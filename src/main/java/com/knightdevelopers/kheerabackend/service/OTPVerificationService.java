package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.EmailRequest;
import com.knightdevelopers.kheerabackend.dto.OtpValidationRequest;
import com.knightdevelopers.kheerabackend.entity.Email;
import com.knightdevelopers.kheerabackend.entity.OneTimePassword;
import com.knightdevelopers.kheerabackend.repository.EmailRepository;
import com.knightdevelopers.kheerabackend.repository.OtpRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Service
public class OTPVerificationService {

    private final OtpRepository otpRepository;
    private final EmailRepository emailRepository;
    private final Clock clock;

    public OTPVerificationService(OtpRepository otpRepository, EmailRepository emailRepository, Clock clock){
        this.otpRepository=otpRepository;
        this.emailRepository=emailRepository;
        this.clock = clock;
    }
    public static long generateOtp() {
        SecureRandom random = new SecureRandom();
        return 100000 + random.nextInt(900000);
    }
    public void sendSignUpOTPtoEmail(EmailRequest userEmail){

        Email newEmail = new Email();
        long otpGenerated= generateOtp();

        newEmail.setRecipientEmail(userEmail.getEmail());

        OneTimePassword otpObject = new OneTimePassword();
        otpObject.setOtp(otpGenerated);
        otpObject.setEmail(userEmail.getEmail());
        otpObject.setExpiresAt(Date.from(clock.instant().plusMillis(1000 * 60 * 15)));
        otpObject.setCreatedAt(Instant.now(clock));

        otpRepository.save(otpObject);




        newEmail.setBody("Hello,\n\n"
                + "Thank you for signing up with us! To complete your registration and confirm your email address, please use this OTP:\n\n"
                + otpGenerated +"\n\n"
                + "If you did not create an account, you can safely ignore this email.\n\n"
                + "Best regards,\n"
                + "The Knight Developers");

        newEmail.setSendAt(new Date());
        newEmail.setIsSent(false);
        newEmail.setIsUrgent(true);
        newEmail.setSubject("Kheera - Account Verification");
        emailRepository.save(newEmail);






    }


    public void sendPasswordResetOTPtoEmail(String userEmail){

        Email newEmail = new Email();
        long otpGenerated= generateOtp();

        newEmail.setRecipientEmail(userEmail);

        OneTimePassword otpObject = new OneTimePassword();
        otpObject.setOtp(otpGenerated);
        otpObject.setEmail(userEmail);
        otpObject.setExpiresAt(Date.from(clock.instant().plusMillis(1000 * 60 * 15)));
        otpObject.setCreatedAt(Instant.now(clock));

        otpRepository.save(otpObject);




        newEmail.setBody("Hello,\n\n"
                + "to change password for your Kheera account, please use this OTP:\n\n"
                + otpGenerated +"\n\n"
                + "If you did not initiate password reset request, you can safely ignore this email.\n\n"
                + "Best regards,\n"
                + "The Knight Developers");

        newEmail.setSendAt(new Date());
        newEmail.setIsSent(false);
        newEmail.setIsUrgent(true);
        newEmail.setSubject("Kheera - Password Reset");
        emailRepository.save(newEmail);






    }

    public boolean validateOtp(OtpValidationRequest otpRequest){
        Optional<OneTimePassword> latestOtp = otpRepository
                .findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(otpRequest.getEmail());

        if (latestOtp.isEmpty()) {
            return false;
        }

        OneTimePassword otp = latestOtp.get();
        return otp.getExpiresAt() != null
                && otp.getExpiresAt().after(Date.from(clock.instant()))
                && Objects.equals(otp.getOtp(), otpRequest.getOtp());


    }
}
