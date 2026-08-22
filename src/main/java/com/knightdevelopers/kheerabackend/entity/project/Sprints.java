package com.knightdevelopers.kheerabackend.entity.project;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "sprints")
public class Sprints extends BaseEntity {
    private  String sprintName;
    private Instant plannedStartDate;
    private  Instant plannedEndDate;
    private Instant actualStartDate;
    private  Instant actualEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private Projects project;
    void assignProject(Projects project){
        this.project=project;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_status_id")
    @Setter(AccessLevel.NONE)
    private ProjectSprintStatus projectSprintStatus;

    void assignProjectSprintStatus(ProjectSprintStatus projectSprintStatus){
        this.projectSprintStatus = projectSprintStatus;
    }
}
