package com.knightdevelopers.kheerabackend.entity;


import jakarta.persistence.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "emails")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    public UUID getId(){
        return id;
    }
    public void setId(UUID id){
        this.id = id;
    }

    private String recipientEmail;
    public String getRecipientEmail(){
        return recipientEmail;
    }
    public void setRecipientEmail(String to){
        this.recipientEmail=to;
    }

    private boolean isUrgent;
    public boolean getIsUrgent(){
        return isUrgent;
    }
    public void setIsUrgent(boolean isUrgent){
        this.isUrgent =isUrgent;
    }

    private String subject;
    public String getSubject(){
        return subject;
    }
    public void setSubject(String subject){
        this.subject=subject;
    }

    private String body;
    public void setBody(String body){
        this.body=body;
    }
    public String getBody(){
        return  body;
    }

    private Date sendAt;
    public void setSendAt(Date sendAt){
        this.sendAt=sendAt;
    }
    public Date getSendAt(){
        return sendAt;
    }

    private boolean isSent;
    public boolean getIsSent(){
        return isSent;
    }
    public void setIsSent(boolean isSent){
        this.isSent=isSent;
    }

    private boolean isFailed;
    public boolean getIsFailed(){
        return  isFailed;
    }
    public void setIsFailed(boolean isFailed){
        this.isFailed=isFailed;
    }

    private int tries;
    public int getTries(){
        return  tries;
    }
    public  void setTries(int tries){
        this.tries=tries;
    }

    private String groupName;
    public String getGroupName(){
        return  groupName;
    }
    public void setGroupName(String groupName){
        this.groupName=groupName;
    }


    private Date createdAt;
    public Date getCreatedAt(){return createdAt;}
}
