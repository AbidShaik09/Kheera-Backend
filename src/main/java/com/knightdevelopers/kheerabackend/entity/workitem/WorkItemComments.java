package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.space.SpaceMembers;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "workItemComment",cascade = CascadeType.ALL,orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<WorkItemCommentAttachments> attachments = new ArrayList<>();

    public void addAttachment(WorkItemCommentAttachments  attachment){
        attachments.add(attachment);
        attachment.assignWorkItemComment(this);
    }
    public void  removeAttachment(WorkItemCommentAttachments attachment){
        attachments.remove(attachment);

    }
}
