package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.service.UserService;
import com.knightdevelopers.kheerabackend.service.OTPVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserService userService;
    private  final OTPVerificationService OTPVerificationService;

    public AuthenticationController(
            UserService userService,
            OTPVerificationService OTPVerificationService
    ) {
        this.userService = userService;
        this.OTPVerificationService = OTPVerificationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try{
            String token= userService.authenticateLoginRequest(request);


            return ResponseEntity.ok(token);

        }
        catch (Exception e){
            return ResponseEntity.status(401).body("Invalid Email Or Password");

        }

    }
    @PostMapping("/signup")
    public ResponseEntity<?> login(@RequestBody SignUpRequest request) {


        if (userService.isAnExistingUser(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered!");
        }


        try{
            String token= userService.createUser(request);

            return ResponseEntity.ok(token);
        }
        catch (Exception e){
            System.out.println("Error while Creating User: "+e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }



    }

    @PostMapping("/signup-email")
    public ResponseEntity<?> signUpWithEmail(@RequestBody EmailRequest userEmail){


        if (userService.isAnExistingUser(userEmail.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered!");
        }

        OTPVerificationService.sendSignUpOTPtoEmail(userEmail);
        return  ResponseEntity.ok("OTP has Been Sent To Your Email");

    }

    @PostMapping("/otp-validation")
    public ResponseEntity<?> validateOtp(@RequestBody OtpValidationRequest otpRequest){
        try{
            boolean isValid =OTPVerificationService.validateOtp(otpRequest);
            if(isValid){
                return  ResponseEntity.ok("Valid OTP, Proceed");

            }
            return ResponseEntity.badRequest().body ("Invalid Or Expired OTP");
        }
        catch (Exception e){

            return ResponseEntity.badRequest().body ("Invalid Or Expired OTP");
        }


    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotUserPassword(@RequestBody EmailRequest userEmail){
        boolean isUserExists=userService.isAnExistingUser(userEmail.getEmail());
        if(isUserExists)
        {
            OTPVerificationService.sendPasswordResetOTPtoEmail(userEmail.getEmail());
        }
        return  ResponseEntity.ok("OTP has Been Sent To Your Email");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetUserPassword(@RequestBody ResetPasswordRequest resetRequest){

        try{
            String token= userService.resetUserPassword(resetRequest);

            return ResponseEntity.ok(token);
        }
        catch (Exception e){
            System.out.println("Error while Resetting Password: "+e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}