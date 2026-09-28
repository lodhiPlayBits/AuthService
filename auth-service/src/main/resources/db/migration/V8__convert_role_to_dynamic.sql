-- Migration to convert role from enum-based to dynamic string-based with system role protection

-- Add new columns
ALTER TABLE role ADD COLUMN name VARCHAR(50);
ALTER TABLE role ADD COLUMN is_system_role BOOLEAN DEFAULT false NOT NULL;

-- Migrate data from role_type to name
UPDATE role SET name = role_type WHERE role_type IS NOT NULL;

-- Mark existing ADMIN and USER as system roles
UPDATE role SET is_system_role = true WHERE name IN ('ADMIN', 'USER');

-- Add constraints to new columns
ALTER TABLE role ALTER COLUMN name SET NOT NULL;
ALTER TABLE role ADD CONSTRAINT uk_role_name UNIQUE (name);

-- Drop old column
ALTER TABLE role DROP COLUMN role_type;
