package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "work_item_types")
public class WorkItemTypes extends BaseEntity {
    @Column(nullable = false)
    private String name;
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private Projects project;

    public void assignProject(Projects project){
        this.project = project;
    }

}
