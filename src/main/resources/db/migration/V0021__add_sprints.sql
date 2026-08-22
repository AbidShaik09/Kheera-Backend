CREATE TABLE sprints (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                         is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                         sprint_name VARCHAR(255),
                         planned_start_date TIMESTAMP,
                         planned_end_date TIMESTAMP,
                         actual_start_date TIMESTAMP,
                         actual_end_date TIMESTAMP,

                         project_id UUID NOT NULL,
                         sprint_status_id UUID,

                         CONSTRAINT fk_sprints_project
                             FOREIGN KEY (project_id)
                                 REFERENCES projects(id),

                         CONSTRAINT fk_sprints_status
                             FOREIGN KEY (sprint_status_id)
                                 REFERENCES project_sprint_status(id)
);