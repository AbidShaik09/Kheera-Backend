package com.knightdevelopers.kheerabackend.entity.space;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import com.knightdevelopers.kheerabackend.entity.project.Projects;
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

    @OneToMany(mappedBy = "space",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<SpacePermissions> permissions = new ArrayList<>();

    public  void addPermission(SpacePermissions permission){
        permissions.add(permission);
        permission.assignToSpace(this);

    }
    public  void removePermission(SpacePermissions permission){
        permissions.remove(permission);
    }

    @OneToMany(mappedBy = "space",cascade = CascadeType.ALL)
    @Setter(AccessLevel.NONE)
    private List<Projects> projects = new ArrayList<>();

    public void addProject(Projects project){
        projects.add(project);
        project.assignToSpace(this);
    }

    public  void removeProject(Projects project){
        projects.remove(project);
    }
}
