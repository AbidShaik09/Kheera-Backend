package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.UserResponse;
import com.knightdevelopers.kheerabackend.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<UserResponse> users =userService.getUsers();
        return  ResponseEntity.ok(users) ;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserFromAuthToken(Authentication authentication){
        if(authentication==null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())){
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Or Expired Token");

        }
        String userEmail=authentication.getName();
        Optional<UserResponse> optionalUser = userService.getCurrentUser(userEmail);
        if (optionalUser.isEmpty()){
            return ResponseEntity.badRequest().body("User not found");
        }
        return ResponseEntity.ok(optionalUser.get());
    }
}
