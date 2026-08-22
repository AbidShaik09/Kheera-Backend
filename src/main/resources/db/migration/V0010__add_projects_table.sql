CREATE TABLE projects(
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                  project_name VARCHAR(255) NOT NULL,
                                  description VARCHAR(500),
                                  space_id UUID NOT NULL,
                                  sprint_cycle_days INT,

                                  CONSTRAINT fk_project_space
                                      FOREIGN KEY (space_id)
                                          REFERENCES spaces(id)
);