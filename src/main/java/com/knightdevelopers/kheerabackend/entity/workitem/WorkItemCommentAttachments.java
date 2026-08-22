package com.knightdevelopers.kheerabackend.entity.workitem;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "work_item_comment_attachments")
public class WorkItemCommentAttachments extends BaseEntity {
    private String name;
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_comment_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private WorkItemComments workItemComment;

    void assignWorkItemComment (WorkItemComments workItemComment){
        this.workItemComment=workItemComment;
    }
}
