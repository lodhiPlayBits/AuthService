-- =====================================================
-- V12: Google OAuth2 Support
-- =====================================================

-- Store the provider-specific unique user ID (Google 'sub' claim)
ALTER TABLE user_table ADD COLUMN provider_id VARCHAR(255);

-- OAuth users don't have a local password
ALTER TABLE user_table ALTER COLUMN password DROP NOT NULL;

-- OAuth users may not provide phone number during initial sign-up
ALTER TABLE user_table ALTER COLUMN phone_number DROP NOT NULL;

-- OAuth users may not provide gender during initial sign-up
ALTER TABLE user_table ALTER COLUMN gender DROP NOT NULL;

-- Track whether user has completed their profile after OAuth sign-up
ALTER TABLE user_table ADD COLUMN profile_complete BOOLEAN NOT NULL DEFAULT true;

-- Unique: one provider + provider_id combination = one user
CREATE UNIQUE INDEX uq_user_provider_id ON user_table (provider, provider_id)
    WHERE provider_id IS NOT NULL;

-- Fast lookup index for OAuth sign-in
CREATE INDEX idx_user_provider_lookup ON user_table (provider, provider_id)
    WHERE provider_id IS NOT NULL;
