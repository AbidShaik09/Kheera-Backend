package com.knightdevelopers.kheerabackend.dto;

public class OtpValidationRequest {
    private String email;
    private Long otp;
    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }

    public long getOtp(){
        return otp;
    }
    public void setOtp(long otp){
        this.otp=otp;
    }
}
