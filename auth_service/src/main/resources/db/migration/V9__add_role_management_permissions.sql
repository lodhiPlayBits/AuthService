-- Add granular permissions for role and permission management

INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'roles:create', 'Create new roles'),
    (gen_random_uuid(), 'roles:read', 'Read roles and their permissions'),
    (gen_random_uuid(), 'roles:update', 'Update roles and assign permissions'),
    (gen_random_uuid(), 'roles:delete', 'Delete custom roles'),
    (gen_random_uuid(), 'permissions:create', 'Create new permissions'),
    (gen_random_uuid(), 'permissions:read', 'Read all permissions'),
    (gen_random_uuid(), 'permissions:delete', 'Delete permissions'),
    (gen_random_uuid(), 'users:assign-roles', 'Assign roles to users');

-- Assign all new permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
AND p.name IN (
    'roles:create',
    'roles:read',
    'roles:update',
    'roles:delete',
    'permissions:create',
    'permissions:read',
    'permissions:delete',
    'users:assign-roles'
);
