package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter
@Setter
@Table(name = "work_item_attachments")
public class WorkItemAttachments extends BaseEntity {
    private String name;
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private WorkItems  workItem;
    void assignWorkItem(WorkItems workItem){
        this.workItem=workItem;
    }
}
