CREATE TABLE project_workflows (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                   is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                   workflow_name VARCHAR(255),
                                   icon VARCHAR(255),
                                   project_id UUID NOT NULL,

                                   CONSTRAINT fk_project_workflows_project
                                       FOREIGN KEY (project_id)
                                           REFERENCES projects(id)
);