package com.knightdevelopers.kheerabackend.entity.project;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.space.Spaces;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "projects")
public class Projects extends BaseEntity {
    @Column(nullable = false)
    private  String projectName;

    private  String description;
    private  Integer sprintCycleDays = 7;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false,name="space_id")
    @Setter(AccessLevel.NONE)
    private Spaces space;

    public void assignToSpace(Spaces space){
        this.space = space;
    }




}
