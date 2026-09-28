-- Add description column to role table
ALTER TABLE role ADD COLUMN IF NOT EXISTS description VARCHAR(255);

-- Update existing roles with descriptions
UPDATE role SET description = 'Standard user with basic permissions' WHERE role_type = 'USER';
UPDATE role SET description = 'Administrator with full system access' WHERE role_type = 'ADMIN';
