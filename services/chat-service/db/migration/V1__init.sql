-- Schema creation only. DB/user provisioning is handled externally (see
-- scripts/provision-test-db.sh or operator bootstrap).
USE chat_service;

CREATE TABLE IF NOT EXISTS chat_channels (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel_key VARCHAR(128) NOT NULL UNIQUE,
  created_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel_id BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_members_channel (channel_id),
  CONSTRAINT fk_chat_members_channel FOREIGN KEY (channel_id) REFERENCES chat_channels(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel_id BIGINT NOT NULL,
  sender_id VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  trace_id VARCHAR(128) NULL,
  sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_messages_channel (channel_id),
  CONSTRAINT fk_chat_messages_channel FOREIGN KEY (channel_id) REFERENCES chat_channels(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_moderation_flags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  message_id BIGINT NOT NULL,
  flagger_id VARCHAR(255) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  trace_id VARCHAR(128) NULL,
  CONSTRAINT fk_chat_moderation_message FOREIGN KEY (message_id) REFERENCES chat_messages(id)
) ENGINE=InnoDB;
