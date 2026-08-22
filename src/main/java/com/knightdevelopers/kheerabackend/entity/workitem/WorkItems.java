package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
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


}
