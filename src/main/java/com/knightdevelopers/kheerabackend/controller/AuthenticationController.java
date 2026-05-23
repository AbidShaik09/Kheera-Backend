package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.LoginRequest;
import com.knightdevelopers.kheerabackend.dto.SignUpRequest;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.service.AuthenticationService;
import com.knightdevelopers.kheerabackend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserService userService;
    private final AuthenticationService AuthenticationService;

    public AuthenticationController(
            UserService userService,
            AuthenticationService AuthenticationService
    ) {
        this.userService = userService;
        this.AuthenticationService = AuthenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        var userOptional = userService.getUserByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid email");
        }

        User user = userOptional.get();

        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid password");
        }

        String token = AuthenticationService.generateToken(user.getEmail());

        return ResponseEntity.ok(token);
    }
    @PostMapping("/signup")
    public ResponseEntity<?> login(@RequestBody SignUpRequest request) {

        var userOptional = userService.getUserByEmail(request.getEmail());

        if (userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered!");
        }


        try{
            User createdUser= userService.createUser(request);
            String token = AuthenticationService.generateToken(createdUser.getEmail());

            return ResponseEntity.ok(token);
        }
        catch (Exception e){
            System.out.println("Error while Creating User: "+e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }



    }

}