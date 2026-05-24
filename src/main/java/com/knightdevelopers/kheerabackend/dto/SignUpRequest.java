package com.knightdevelopers.kheerabackend.dto;

public class SignUpRequest {
    private  String email;
    private  String password;
    private  String name;
    private  long otp;

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email=email;
    }

    public String getPassword(){
        return password;
    }
    public  void setPassword(String password){
        this.password=password;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name=name;
    }

    public long getOtp(){
        return otp;
    }
    public  void  setOtp(long otp){
        this.otp=otp;
    }
}
