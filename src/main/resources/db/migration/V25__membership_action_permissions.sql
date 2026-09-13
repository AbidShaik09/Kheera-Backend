-- Preserve existing management authority while separating membership actions.
INSERT INTO space_permissions(space_id, permission_name)
SELECT s.id, action.name FROM spaces s
CROSS JOIN (VALUES ('space.members.read'), ('space.members.add'),
                   ('space.members.change-role'), ('space.members.remove')) action(name)
WHERE s.is_deleted=false AND NOT EXISTS (
    SELECT 1 FROM space_permissions p WHERE p.space_id=s.id AND p.permission_name=action.name
);

INSERT INTO space_role_permissions(space_role_id, space_permission_id)
SELECT DISTINCT r.id, target.id
FROM space_roles r
JOIN spaces s ON s.id=r.space_id AND s.is_deleted=false
JOIN space_role_permissions g ON g.space_role_id=r.id AND g.is_deleted=false
JOIN space_permissions source ON source.id=g.space_permission_id
    AND source.space_id=s.id AND source.is_deleted=false
    AND source.permission_name='space.members.manage'
JOIN space_permissions target ON target.space_id=s.id AND target.is_deleted=false
    AND target.permission_name IN ('space.members.read','space.members.add',
                                  'space.members.change-role','space.members.remove')
WHERE r.is_deleted=false
ON CONFLICT (space_role_id, space_permission_id) DO NOTHING;
