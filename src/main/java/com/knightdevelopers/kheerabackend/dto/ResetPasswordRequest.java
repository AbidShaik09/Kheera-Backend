package com.knightdevelopers.kheerabackend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {
    private String password;
    private String email;
    private long otp;

}
