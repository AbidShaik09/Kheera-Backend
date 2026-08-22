ALTER TABLE work_items
    ADD COLUMN assigned_to_id UUID;

ALTER TABLE work_items
    ADD CONSTRAINT fk_work_items_assigned_to
        FOREIGN KEY (assigned_to_id)
            REFERENCES space_members(id);