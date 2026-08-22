package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.space.SpaceMembers;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "work_item_comments")
public class WorkItemComments extends BaseEntity {
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_member_id")
    @Setter(AccessLevel.NONE)
    private SpaceMembers spaceMember;

    void assignSpaceMember(SpaceMembers spaceMember){
        this.spaceMember=spaceMember;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private WorkItems workItem;
    void assignWorkItem(WorkItems workItem){
        this.workItem=workItem;
    }
}
