package com.knightdevelopers.kheerabackend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SignUpRequest {
    private  String email;
    private  String password;
    private  String name;
    private  long otp;

}
