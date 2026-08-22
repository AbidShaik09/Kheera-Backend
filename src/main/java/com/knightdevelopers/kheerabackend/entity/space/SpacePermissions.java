package com.knightdevelopers.kheerabackend.entity.space;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="space_permissions")
@Getter
@Setter
public class SpacePermissions extends BaseEntity {
    @Column(nullable = false)
    private String permissionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id",nullable = false)
    @Setter(AccessLevel.NONE)
    private Spaces space;

    public  void assignToSpace(Spaces space){
        this.space=space;
    }

}
