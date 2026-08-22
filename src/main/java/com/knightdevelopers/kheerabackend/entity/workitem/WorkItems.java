package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
import com.knightdevelopers.kheerabackend.entity.space.SpaceMembers;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_items")
@Setter
@Getter
public class WorkItems extends BaseEntity {
    private String title;
    private String description;
    private Integer efforts=1;
    private Instant plannedStartDate;
    private Instant plannedEndDate;
    private Instant actualStartDate;
    private Instant actualEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private Projects project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id")
    private WorkItems parentItem;

    @OneToMany(mappedBy = "parentItem")
    private List<WorkItems> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_type_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private WorkItemTypes workItemType;

    public void setWorkItemType(WorkItemTypes workItemType) {
        if (workItemType == null) {
            throw new IllegalArgumentException("workItemType cannot be null");
        }

        if (!workItemType.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException(
                    "work_item_type does not belong to project"
            );
        }

        this.workItemType = workItemType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    @Setter(AccessLevel.NONE)
    private SpaceMembers spaceMember;

    public void  assignToSpaceMember(SpaceMembers spaceMember){
        this.spaceMember = spaceMember;
    }
    public void unassignSpaceMember(){
        this.spaceMember= null;
    }



}
