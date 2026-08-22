package com.knightdevelopers.kheerabackend.entity;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "spaces")
public class Spaces extends BaseEntity {

    private String spaceName;
    private String description;
    private  String profilePic;
}
