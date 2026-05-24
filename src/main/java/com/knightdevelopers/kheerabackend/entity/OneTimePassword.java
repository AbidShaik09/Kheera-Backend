package com.knightdevelopers.kheerabackend.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.UUID;
//
//@Entity
//@Table(name = "OneTimePasswords")
public class OneTimePassword {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private  String email;
    private  long otp;
    private Date expiresAt;
    private int tries;
    private  Date createdAt;

    public UUID getId(){
        return id;
    }
    public  void setId(UUID id){
        this.id=id;
    }
    public  String getEmail(){
        return email;

    }
    public long getOtp(){
        return  otp;
    }
    public void setOtp(long otp){
        this.otp=otp;
    }
    public void setExpiresAt(Date expiresAt){
        this.expiresAt=expiresAt;
    }
    public  Date getExpiresAt(){
        return  expiresAt;
    }

}
