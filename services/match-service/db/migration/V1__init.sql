-- Schema creation only. DB/user provisioning is handled externally (see
-- scripts/provision-test-db.sh or operator bootstrap).
USE match_service;

CREATE TABLE IF NOT EXISTS matches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  external_match_id VARCHAR(64) NOT NULL UNIQUE,
  shard_region VARCHAR(64) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  trace_id VARCHAR(128) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS match_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT NOT NULL,
  closure_reason VARCHAR(64) NOT NULL,
  ended_at TIMESTAMP NOT NULL,
  trace_id VARCHAR(128) NULL,
  CONSTRAINT fk_match_results_match FOREIGN KEY (match_id) REFERENCES matches(id),
  CONSTRAINT uq_match_result UNIQUE (match_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS match_player_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  match_result_id BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  score INT NOT NULL,
  winner BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_player_results_result FOREIGN KEY (match_result_id) REFERENCES match_results(id),
  INDEX idx_player_results_match (match_result_id),
  INDEX idx_player_results_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS match_outbox (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  match_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload JSON NOT NULL,
  trace_id VARCHAR(128) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP NULL,
  INDEX idx_match_outbox_trace (trace_id),
  CONSTRAINT fk_match_outbox_match FOREIGN KEY (match_id) REFERENCES matches(id)
) ENGINE=InnoDB;
