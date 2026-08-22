CREATE TABLE space_roles (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                             is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                             role_name VARCHAR(255) NOT NULL,
                             space_id UUID NOT NULL,

                             CONSTRAINT fk_space_roles_space
                                 FOREIGN KEY (space_id)
                                     REFERENCES spaces(id)
);