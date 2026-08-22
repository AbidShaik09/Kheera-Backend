CREATE TABLE space_permissions(
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                             is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                             permission_name VARCHAR(255) NOT NULL,
                             space_id UUID NOT NULL,

                             CONSTRAINT fk_space_permissions_space
                                 FOREIGN KEY (space_id)
                                     REFERENCES spaces(id)
);