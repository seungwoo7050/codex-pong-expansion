-- Schema creation only. DB/user provisioning is handled externally (see
-- scripts/provision-test-db.sh or operator bootstrap).
USE identity_service;

CREATE TABLE IF NOT EXISTS identity_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identity_sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  access_token VARCHAR(512) NOT NULL UNIQUE,
  refresh_token VARCHAR(512) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_identity_sessions_user (user_id),
  CONSTRAINT fk_identity_sessions_user FOREIGN KEY (user_id) REFERENCES identity_users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identity_key_set (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  kid VARCHAR(128) NOT NULL UNIQUE,
  public_key TEXT NOT NULL,
  algorithm VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  rotated_at TIMESTAMP NULL,
  trace_id VARCHAR(128) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identity_key_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  kid VARCHAR(128) NOT NULL,
  action VARCHAR(64) NOT NULL,
  actor VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  trace_id VARCHAR(128) NULL,
  INDEX idx_identity_key_audit_kid (kid)
) ENGINE=InnoDB;
