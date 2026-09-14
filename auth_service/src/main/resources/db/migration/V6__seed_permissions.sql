-- Insert permissions
INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'user:read', 'Read user profile'),
    (gen_random_uuid(), 'user:update', 'Update own profile'),
    (gen_random_uuid(), 'user:delete', 'Delete own account'),
    (gen_random_uuid(), 'admin:read', 'Read all users'),
    (gen_random_uuid(), 'admin:create', 'Create new users'),
    (gen_random_uuid(), 'admin:update', 'Update any user'),
    (gen_random_uuid(), 'admin:delete', 'Delete any user'),
    (gen_random_uuid(), 'admin:role:manage', 'Manage user roles'),
    (gen_random_uuid(), 'audit:read', 'Read audit logs'),
    (gen_random_uuid(), 'audit:manage', 'Manage audit logs');

-- Assign permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permissions p
WHERE r.role_type = 'USER'
AND p.name IN ('user:read', 'user:update', 'user:delete');

-- Assign permissions to ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permissions p
WHERE r.role_type = 'ADMIN';
