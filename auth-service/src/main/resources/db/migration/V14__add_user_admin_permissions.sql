-- Permissions required by AdminController user-management endpoints.
-- These were referenced by @RequiresPermission("users:read"/"users:update")
-- but never seeded, so no role could ever satisfy them.
INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'users:read', 'List all users'),
    (gen_random_uuid(), 'users:update', 'Enable/disable user accounts');

-- Assign the new permissions to the ADMIN role (dynamic role name since V8)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
AND p.name IN ('users:read', 'users:update');
