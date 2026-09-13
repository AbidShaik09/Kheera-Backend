package com.knightdevelopers.kheerabackend.entity.space;

import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "space_members")
public class SpaceMembers extends BaseEntity {

    public static SpaceMembers create(User user, Spaces space, SpaceRoles role) {
        SpaceMembers member = new SpaceMembers();
        member.user = user; member.space = space; member.changeRole(role);
        return member;
    }

    public void changeRole(SpaceRoles role) {
        if (!space.getId().equals(role.getSpace().getId())) throw new IllegalArgumentException("Role belongs to another space");
        this.spaceRole = role;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private User user;
    void assignUser(User user){
        this.user = user;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Spaces space;
    void assignSpace(Spaces space){
        this.space=space;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_role_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private SpaceRoles spaceRole;
    void assignSpaceRole(SpaceRoles spaceRole){
        this.spaceRole =spaceRole;
    }
}
