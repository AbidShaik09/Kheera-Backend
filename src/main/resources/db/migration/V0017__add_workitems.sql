CREATE TABLE work_items (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                            is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                            title VARCHAR(255),
                            description VARCHAR(500),
                            efforts INT DEFAULT 1,

                            planned_start_date TIMESTAMP,
                            planned_end_date TIMESTAMP,
                            actual_start_date TIMESTAMP,
                            actual_end_date TIMESTAMP,

                            project_id UUID NOT NULL,
                            parent_item_id UUID,

                            CONSTRAINT fk_work_items_project
                                FOREIGN KEY (project_id)
                                    REFERENCES projects(id),

                            CONSTRAINT fk_work_items_parent
                                FOREIGN KEY (parent_item_id)
                                    REFERENCES work_items(id)
);