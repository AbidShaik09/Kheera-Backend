-- Keep historical items/types intact. Supply a usable type to projects without one.
CREATE FUNCTION provision_project_task_type(p_project_id UUID) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO work_item_types(project_id,name)
    SELECT p_project_id,'Task'
    WHERE NOT EXISTS(SELECT 1 FROM work_item_types WHERE project_id=p_project_id AND NOT is_deleted);
END;
$$;
DO $$ DECLARE p RECORD; BEGIN
    FOR p IN SELECT id FROM projects LOOP PERFORM provision_project_task_type(p.id); END LOOP;
END $$;
CREATE FUNCTION initialize_project_task_type() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN PERFORM provision_project_task_type(NEW.id); RETURN NEW; END;
$$;
CREATE TRIGGER initialize_project_task_type AFTER INSERT ON projects
FOR EACH ROW EXECUTE FUNCTION initialize_project_task_type();
CREATE INDEX idx_work_items_parent ON work_items(parent_item_id);
CREATE INDEX idx_work_items_type ON work_items(project_id,work_item_type_id) WHERE NOT is_deleted;
CREATE INDEX idx_work_items_assignee ON work_items(project_id,assigned_to_id) WHERE NOT is_deleted;
CREATE INDEX idx_work_item_types_project ON work_item_types(project_id,is_deleted,name,id);
