CREATE TABLE project_sprint_status (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                       is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                       status_name VARCHAR(255) NOT NULL,
                                       project_id UUID NOT NULL,

                                       CONSTRAINT fk_project_sprint_status_project
                                           FOREIGN KEY (project_id)
                                               REFERENCES projects(id)
);