package com.knightdevelopers.kheerabackend.entity.project;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "project_workflows")
public class ProjectWorkflows extends BaseEntity {
    private  String workflowName;
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false,name = "project_id")
    @Setter(AccessLevel.NONE)
    private  Projects project;

    void assignProject(Projects project){
        this.project = project;
    }
}
