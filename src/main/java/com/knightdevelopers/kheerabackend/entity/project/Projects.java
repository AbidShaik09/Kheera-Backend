package com.knightdevelopers.kheerabackend.entity.project;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.space.Spaces;
import com.knightdevelopers.kheerabackend.entity.workitem.WorkItemTypes;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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


    @OneToMany(mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<ProjectSprintStatus> sprintStatuses= new ArrayList<>();

    public void addSprintStatus(ProjectSprintStatus sprintStatus){
        this.sprintStatuses.add(sprintStatus);
        sprintStatus.assignProject(this);
    }
    public void removeSprintStatus(ProjectSprintStatus sprintStatus){
        this.sprintStatuses.remove(sprintStatus);
    }

    @OneToMany(mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<ProjectURLs> projectURLs= new ArrayList<>();

    public void addProjectURL(ProjectURLs projectURL){
        projectURLs.add(projectURL);
        projectURL.assignProject(this);
    }
    public void  removeProjectURL(ProjectURLs projectURL){
        projectURLs.remove(projectURL);

    }


    @OneToMany(mappedBy = "project",
        cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Setter(AccessLevel.NONE)
    private List<ProjectWorkflows> workflows= new ArrayList<>();

    public void addWorkflow(ProjectWorkflows workflow) {
        workflows.add(workflow);
        workflow.assignProject(this);
    }

    public void removeWorkflow(ProjectWorkflows workflow) {
        workflows.remove(workflow);
    }

    @OneToMany(mappedBy = "project",cascade = CascadeType.ALL,orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private  List<WorkItemTypes> workItemTypes = new ArrayList<>();

    public void addWorkItemType(WorkItemTypes workItemType){
        workItemTypes.add(workItemType);
        workItemType.assignProject(this);
    }
    public void  removeWorkItemType(WorkItemTypes workItemType){
        workItemTypes.remove(workItemType);
    }



}
