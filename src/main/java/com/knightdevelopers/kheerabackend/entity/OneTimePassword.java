package com.knightdevelopers.kheerabackend.entity;
import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import lombok.Getter;
import jakarta.persistence.*;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "OneTimePasswords")
public class OneTimePassword extends BaseEntity {

    private  String email;
    private  Long otp;
    private Date expiresAt;
    private Integer tries = 0;

}
