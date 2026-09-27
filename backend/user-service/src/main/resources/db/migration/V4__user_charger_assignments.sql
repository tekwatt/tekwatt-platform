CREATE TABLE user_charger_assignments (
  user_id BINARY(16) NOT NULL,
  charger_id BINARY(16) NOT NULL,
  PRIMARY KEY (user_id, charger_id),
  CONSTRAINT fk_user_charger_assignments_user FOREIGN KEY (user_id) REFERENCES user_profiles(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_charger_assignments_charger_id ON user_charger_assignments(charger_id);
