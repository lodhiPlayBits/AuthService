CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    jti VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    family_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replace_token VARCHAR(255),
    
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(user_id)
        ON DELETE CASCADE
);

CREATE INDEX refresh_token_jti_idx ON refresh_tokens(jti);
CREATE INDEX refresh_token_user_id_idx ON refresh_tokens(user_id);
