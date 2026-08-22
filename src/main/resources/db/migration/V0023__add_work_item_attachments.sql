CREATE TABLE work_item_attachments (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                       is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                       name VARCHAR(255),
                                       url VARCHAR(2048),
                                       work_item_id UUID NOT NULL,

                                       CONSTRAINT fk_work_item_attachments_work_item
                                           FOREIGN KEY (work_item_id)
                                               REFERENCES work_items(id)
);