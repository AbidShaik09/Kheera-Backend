CREATE TABLE space_role_permissions (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                        is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Relationships
                                        space_role_id UUID NOT NULL,
                                        space_permission_id UUID NOT NULL,

                                        CONSTRAINT fk_space_role_permissions_role
                                            FOREIGN KEY (space_role_id)
                                                REFERENCES space_roles(id),

                                        CONSTRAINT fk_space_role_permissions_permission
                                            FOREIGN KEY (space_permission_id)
                                                REFERENCES space_permissions(id),

                                        CONSTRAINT uq_space_role_permission
                                            UNIQUE (space_role_id, space_permission_id)
);