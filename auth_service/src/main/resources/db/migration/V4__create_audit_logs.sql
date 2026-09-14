CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    success BOOLEAN NOT NULL,
    
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(user_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
