CREATE TABLE work_item_comments (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- BaseEntity
                                    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
                                    description VARCHAR(500),
                                    space_member_id UUID,
                                    work_item_id UUID NOT NULL,

                                    CONSTRAINT fk_work_item_comments_space_member
                                        FOREIGN KEY (space_member_id)
                                            REFERENCES space_members(id),

                                    CONSTRAINT fk_work_item_comments_work_item
                                        FOREIGN KEY (work_item_id)
                                            REFERENCES work_items(id)
);