package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.SignUpRequest;
import com.knightdevelopers.kheerabackend.dto.UserResponse;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }



    public Optional<User> getUserByEmail(String email){
        return userRepository.findByEmail(email);
    }
    public User createUser(SignUpRequest user){
        User newUser = new User(
                user.getEmail(),
                user.getPassword(),
                user.getName()
        );

        return userRepository.save(newUser);

    }
    @GetMapping
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
