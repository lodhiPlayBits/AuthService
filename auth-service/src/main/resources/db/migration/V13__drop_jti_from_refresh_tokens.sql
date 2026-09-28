-- Drop the plaintext JTI column from refresh_tokens table to complete the migration
-- toward hashed JTI storage (jti_hash). 
-- This aligns the DB schema with the updated Java RefreshToken model.

ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS jti;
