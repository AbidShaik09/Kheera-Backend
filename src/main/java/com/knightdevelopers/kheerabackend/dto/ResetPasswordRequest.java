package com.knightdevelopers.kheerabackend.dto;

public class ResetPasswordRequest {
    private String password;
    private String email;
    private long otp;

    public String getPassword(){
        return password;
    }
    public String getEmail(){
        return email;
    }
    public long getOtp(){
        return otp;
    }

    public void setPassword(String password){
        this.password=password;
    }
    public void setOtp(long otp){
        this.otp=otp;
    }
    public void setEmail(String email){
        this.email=email;
    }
}
