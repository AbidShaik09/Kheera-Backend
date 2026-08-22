CREATE TABLE work_item_comment_attachments (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                               is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                               name VARCHAR(255),
                                               url VARCHAR(2048),
                                               work_item_comment_id UUID NOT NULL,

                                               CONSTRAINT fk_work_item_comment_attachments_comment
                                                   FOREIGN KEY (work_item_comment_id)
                                                       REFERENCES work_item_comments(id)
);