CREATE TABLE admin_actions (
 id BINARY(16) PRIMARY KEY,
 actor_id BINARY(16) NOT NULL,
 action_type VARCHAR(40) NOT NULL,
 target_id VARCHAR(100) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 request_payload TEXT,
 status VARCHAR(20) NOT NULL,
 response_status INT,
 response_payload TEXT,
 error_message VARCHAR(1000),
 created_at TIMESTAMP(6) NOT NULL,
 completed_at TIMESTAMP(6)
);
CREATE INDEX idx_admin_actions_actor_created ON admin_actions(actor_id,created_at);
CREATE INDEX idx_admin_actions_target ON admin_actions(target_id,action_type);
