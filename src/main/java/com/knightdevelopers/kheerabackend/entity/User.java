package com.knightdevelopers.kheerabackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String password;
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
    public  Long getId(){
        return  id;
    }

//    Setters
    public void setPassword(String newPassword){
        password=newPassword;
    }
    public void setName(String newName){
        name=newName;
    }
    public void setEmail(String newEmail){
        email=newEmail;
    }
}