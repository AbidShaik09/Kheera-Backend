package com.knightdevelopers.kheerabackend.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "emails")
@Getter
@Setter
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String recipientEmail;
    private Boolean isUrgent;
    private String subject;
    private String body;
    private Date sendAt;
    private Boolean isSent;
    private Boolean isFailed;
    private Integer tries;
    private String groupName;
    private Date createdAt;
}
