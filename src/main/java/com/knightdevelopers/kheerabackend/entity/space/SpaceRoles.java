package com.knightdevelopers.kheerabackend.entity.space;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "space_roles")
public class SpaceRoles extends BaseEntity {

    private String roleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Spaces space;


    public void assignToSpace(Spaces space) {
        this.space = space;
    }


    @OneToMany(
            mappedBy = "spaceRole",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SpaceRolePermissions> rolePermissions = new ArrayList<>();


}
