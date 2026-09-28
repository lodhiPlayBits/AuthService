-- V10: Fix role permissions seeding for dynamic roles
-- This compensates for V6 being run with role_type before the V8 migration

-- First, clear any existing role_permissions that might be incorrect
-- (only if your database has issues from the old V6)

-- Re-assign permissions to USER role using the new 'name' column
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permissions p
WHERE r.name = 'USER'
AND p.name IN ('user:read', 'user:update', 'user:delete')
ON CONFLICT DO NOTHING;  -- Skip if already exists

-- Re-assign all permissions to ADMIN role using the new 'name' column
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;  -- Skip if already exists
