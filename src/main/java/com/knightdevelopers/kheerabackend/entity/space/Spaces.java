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
@Table(name = "spaces")
public class Spaces extends BaseEntity {

    private String spaceName;
    private String description;
    private  String profilePic;

    @OneToMany(mappedBy = "space",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<SpaceRoles> roles = new ArrayList<>();


    public void addRole(SpaceRoles role) {
        roles.add(role);
        role.assignToSpace(this);
    }

    public void removeRole(SpaceRoles role) {
        roles.remove(role);
    }
}
