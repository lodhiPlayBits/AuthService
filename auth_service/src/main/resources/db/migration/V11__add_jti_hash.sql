-- Add jti_hash column for secure lookup
-- JTI will still be stored for reference, but lookups will use the hash

ALTER TABLE refresh_tokens
    ADD COLUMN jti_hash VARCHAR(64);

-- Create unique index on jti_hash for fast secure lookups
CREATE UNIQUE INDEX idx_refresh_token_jti_hash ON refresh_tokens(jti_hash);

-- Note: Existing tokens without jti_hash will need to be regenerated
-- For migration safety, we keep the existing jti column and index
-- Production deployment should:
-- 1. Deploy this migration
-- 2. Force all users to re-login (or wait for natural token expiration)
-- 3. After grace period, existing tokens without jti_hash will be rejected
