ALTER TABLE work_items
    ADD COLUMN work_item_type_id UUID;

ALTER TABLE work_items
    ADD CONSTRAINT fk_work_items_work_item_type
        FOREIGN KEY (work_item_type_id)
            REFERENCES work_item_types(id);