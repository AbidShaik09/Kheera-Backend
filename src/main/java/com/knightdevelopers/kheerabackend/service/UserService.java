package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OTPVerificationService OTPVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    public UserService(UserRepository userRepository, OTPVerificationService OTPVerificationService, PasswordEncoder passwordEncoder, AuthenticationService authenticationService){
        this.userRepository=userRepository;
        this.OTPVerificationService = OTPVerificationService;
        this.passwordEncoder=passwordEncoder;
        this.authenticationService=authenticationService;
    }

    public  String resetUserPassword (ResetPasswordRequest resetPasswordRequest) throws  Exception{
        OtpValidationRequest otpValidationRequest=new OtpValidationRequest();
        otpValidationRequest.setOtp(resetPasswordRequest.getOtp());
        otpValidationRequest.setEmail(resetPasswordRequest.getEmail());
        boolean isUserEmailVerified= OTPVerificationService.validateOtp(otpValidationRequest) ;
        if (!isUserEmailVerified){
            throw new Exception("OTP is not verified");
        }

        User existingUser= userRepository.findByEmail(resetPasswordRequest.getEmail()).get();
        existingUser.setPassword(passwordEncoder.encode(resetPasswordRequest.getPassword()));
        userRepository.save(existingUser);

        return authenticationService.generateToken(existingUser.getEmail());

    }
    public String authenticateLoginRequest(LoginRequest loginRequest) throws Exception {
        boolean isUserExists= userRepository.findByEmail(loginRequest.getEmail()).isPresent();
        if(!isUserExists){
            throw new Exception("Invalid Email Or Password");
        }
        User existingUser=userRepository.findByEmail(loginRequest.getEmail()).get();
        boolean isAuthenticated= passwordEncoder.matches(loginRequest.getPassword(), existingUser.getPassword());
        if (!isAuthenticated){

            throw new Exception("Invalid Email Or Password");
        }

        return authenticationService.generateToken(existingUser.getEmail());
    }

    public Optional<User> getUserByEmail(String email){

        return userRepository.findByEmail(email);
    }

    public boolean isAnExistingUser(String email){
        return userRepository.findByEmail(email).isPresent();
    }
    public String createUser(SignUpRequest signUpRequest) throws Exception {

        OtpValidationRequest otpValidationRequest=new OtpValidationRequest();
        otpValidationRequest.setOtp(signUpRequest.getOtp());
        otpValidationRequest.setEmail(signUpRequest.getEmail());
        boolean isUserEmailVerified= OTPVerificationService.validateOtp(otpValidationRequest) ;

        if (!isUserEmailVerified){
            throw new Exception("User Email is not verified");
        }
        User newUser = new User();
        newUser.setName(signUpRequest.getName());
        newUser.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        newUser.setEmail(signUpRequest.getEmail());

        User createdUser= userRepository.save(newUser);
        return authenticationService.generateToken(createdUser.getEmail());

    }

    public List<UserResponse> getUsers() {


        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                ))
                .toList();
    }
}
