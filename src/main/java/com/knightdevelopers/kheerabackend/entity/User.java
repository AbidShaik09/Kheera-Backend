package com.knightdevelopers.kheerabackend.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String email;

    private String password;
    private Date updatedAt;
    private Date createdAt;
    public User() {
    }

//    Constructor
    public User(String email,String password,String name){
        this.email = email;
        this.password=password;
        this.name=name;
    }
//  Getters
    public String getPassword(){
        return  password;
    }

    public String getEmail() {
        return email;
    }
    public  String getName(){
        return  name;
    }
    public  Date getCreatedAt(){
        return  createdAt;
    }
    public  Date getUpdatedAt(){
        return  updatedAt;
    }
    public  UUID getId(){
        return  id;
    }

//    Setters
public void setPassword(String newPassword){
    password=newPassword;
}
    public void setCreatedAt(Date newDate){
        createdAt=newDate;
    }
    public void setUpdatedAt(Date updatedDate){
        updatedAt=updatedDate;
    }
    public void setName(String newName){
        name=newName;
    }
    public void setEmail(String newEmail){
        email=newEmail;
    }
}