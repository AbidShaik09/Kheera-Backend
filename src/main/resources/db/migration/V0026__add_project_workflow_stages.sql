-- A project_workflows row is one board stage, not a container of stages.
ALTER TABLE project_workflows ADD COLUMN position INTEGER;
ALTER TABLE project_workflows ADD COLUMN is_complete BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE project_workflows SET workflow_name = 'Unnamed stage' WHERE workflow_name IS NULL;
WITH numbered AS (
    SELECT id, row_number() OVER (PARTITION BY project_id, is_deleted ORDER BY created_at, id) - 1 AS ordinal
    FROM project_workflows
)
UPDATE project_workflows w SET position = n.ordinal FROM numbered n WHERE w.id = n.id;

CREATE FUNCTION provision_project_board(p_project_id UUID) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO project_workflows(project_id, workflow_name, position, is_complete)
    SELECT p_project_id, name, ordinal, complete
    FROM (VALUES ('Backlog',0,FALSE),('To Do',1,FALSE),('In Progress',2,FALSE),
                 ('In Review',3,FALSE),('Done',4,TRUE),('Blocked',5,FALSE)) AS defaults(name,ordinal,complete)
    WHERE NOT EXISTS (SELECT 1 FROM project_workflows WHERE project_id=p_project_id AND NOT is_deleted);
END;
$$;
-- Preserve existing custom stages and their IDs; legacy completion is unknown/false.
DO $$ DECLARE p RECORD; BEGIN
    FOR p IN SELECT id FROM projects LOOP PERFORM provision_project_board(p.id); END LOOP;
END $$;

ALTER TABLE project_workflows ALTER COLUMN workflow_name SET NOT NULL;
ALTER TABLE project_workflows ALTER COLUMN position SET NOT NULL;
ALTER TABLE project_workflows ADD CONSTRAINT ck_workflow_position CHECK(position >= 0);
ALTER TABLE project_workflows ADD CONSTRAINT uq_workflow_project UNIQUE(id,project_id);
ALTER TABLE project_workflows ADD COLUMN active_position INTEGER GENERATED ALWAYS AS
    (CASE WHEN NOT is_deleted THEN position ELSE NULL END) STORED;
ALTER TABLE project_workflows ADD CONSTRAINT uq_workflow_active_position UNIQUE(project_id,active_position) DEFERRABLE INITIALLY DEFERRED;

CREATE FUNCTION initialize_project_board() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN PERFORM provision_project_board(NEW.id); RETURN NEW; END;
$$;
CREATE TRIGGER initialize_project_board AFTER INSERT ON projects
FOR EACH ROW EXECUTE FUNCTION initialize_project_board();

ALTER TABLE work_items ADD COLUMN workflow_id UUID;
ALTER TABLE work_items ADD COLUMN position INTEGER;
UPDATE work_items item SET workflow_id = (
    SELECT w.id FROM project_workflows w WHERE w.project_id=item.project_id AND NOT w.is_deleted ORDER BY w.position,w.id LIMIT 1
);
WITH numbered AS (
    SELECT id, row_number() OVER(PARTITION BY workflow_id,is_deleted ORDER BY created_at,id)-1 AS ordinal FROM work_items
)
UPDATE work_items w SET position=n.ordinal FROM numbered n WHERE w.id=n.id;
ALTER TABLE work_items ALTER COLUMN workflow_id SET NOT NULL;
ALTER TABLE work_items ALTER COLUMN position SET NOT NULL;
ALTER TABLE work_items ADD CONSTRAINT ck_work_item_position CHECK(position>=0);
ALTER TABLE work_items ADD CONSTRAINT fk_work_item_project_stage FOREIGN KEY(workflow_id,project_id)
    REFERENCES project_workflows(id,project_id);
ALTER TABLE work_items ADD COLUMN active_position INTEGER GENERATED ALWAYS AS
    (CASE WHEN NOT is_deleted THEN position ELSE NULL END) STORED;
ALTER TABLE work_items ADD CONSTRAINT uq_work_item_active_position UNIQUE(workflow_id,active_position) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX idx_work_items_board ON work_items(project_id,is_deleted,workflow_id,position,id);

-- SQL fixtures and future create APIs receive a valid initial column and append position.
-- All board writers serialize space -> project before touching stage/item rows.
CREATE FUNCTION validate_work_item_stage() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.workflow_id IS NULL THEN
        SELECT id INTO NEW.workflow_id FROM project_workflows
        WHERE project_id=NEW.project_id AND NOT is_deleted ORDER BY position,id LIMIT 1;
    END IF;
    IF NOT NEW.is_deleted THEN
        -- Coordinate with stage deletion even for SQL writers outside the service.
        PERFORM 1 FROM project_workflows
        WHERE id=NEW.workflow_id AND project_id=NEW.project_id AND NOT is_deleted FOR SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'An active work item requires an active stage in its project' USING ERRCODE='23514';
        END IF;
    END IF;
    IF NEW.position IS NULL THEN
        SELECT COALESCE(MAX(position)+1,0) INTO NEW.position FROM work_items WHERE workflow_id=NEW.workflow_id AND NOT is_deleted;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER validate_work_item_stage BEFORE INSERT OR UPDATE ON work_items
FOR EACH ROW EXECUTE FUNCTION validate_work_item_stage();

CREATE FUNCTION protect_nonempty_stage() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.is_deleted AND NOT OLD.is_deleted AND EXISTS (
        SELECT 1 FROM work_items WHERE workflow_id=OLD.id AND NOT is_deleted
    ) THEN RAISE EXCEPTION 'Cannot delete a nonempty stage' USING ERRCODE='23514'; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER protect_nonempty_stage BEFORE UPDATE OF is_deleted ON project_workflows
FOR EACH ROW EXECUTE FUNCTION protect_nonempty_stage();
