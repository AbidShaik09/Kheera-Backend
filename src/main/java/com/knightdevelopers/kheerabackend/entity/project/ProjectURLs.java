package com.knightdevelopers.kheerabackend.entity.project;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "project_urls")
public class ProjectURLs extends BaseEntity {
    private String url;
    private String displayName;
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private  Projects project;

    void assignProject(Projects project){
        this.project=project;
    }
}
