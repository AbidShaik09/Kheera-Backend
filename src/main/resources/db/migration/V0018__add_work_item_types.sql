CREATE TABLE work_item_types (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                 is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                 name VARCHAR(255) NOT NULL,
                                 icon VARCHAR(255),
                                 project_id UUID,

                                 CONSTRAINT fk_work_item_types_project
                                     FOREIGN KEY (project_id)
                                         REFERENCES projects(id)
);