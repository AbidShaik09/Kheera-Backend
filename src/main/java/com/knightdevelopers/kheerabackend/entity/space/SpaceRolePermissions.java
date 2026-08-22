package com.knightdevelopers.kheerabackend.entity.space;

import com.knightdevelopers.kheerabackend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "space_role_permissions")
public class SpaceRolePermissions extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_role_id", nullable = false)
    private SpaceRoles spaceRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_permission_id", nullable = false)
    private SpacePermissions spacePermission;
}
