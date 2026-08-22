package com.knightdevelopers.kheerabackend.entity;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.space.SpaceMembers;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User extends BaseEntity {

    private String name;
    private String email;
    private String password;
    private String profilePic;
    public User() {
    }

//    Constructor
    public User(String email,String password,String name){
        this.email = email;
        this.password=password;
        this.name=name;
    }

    @OneToMany(mappedBy = "user")
    @Setter(AccessLevel.NONE)
    private List<SpaceMembers> spaceMembers = new ArrayList<>();


}