package com.knightdevelopers.kheerabackend.dto;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class OtpValidationRequest {

    private String email;
    private Long otp;
}
