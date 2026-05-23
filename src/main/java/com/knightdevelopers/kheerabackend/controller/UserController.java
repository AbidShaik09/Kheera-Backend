package com.knightdevelopers.kheerabackend.controller;

import com.knightdevelopers.kheerabackend.dto.UserResponse;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.repository.UserRepository;
import com.knightdevelopers.kheerabackend.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService _userService)
    {
        this.userService=_userService;
    }
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<UserResponse> users =userService.getUsers();
        return  ResponseEntity.ok(users) ;
    }
}
