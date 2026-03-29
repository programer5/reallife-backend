CREATE TABLE playback_sessions (
    id BINARY(16) NOT NULL,
    conversation_id BINARY(16) NOT NULL,
    host_user_id BINARY(16) NOT NULL,
    media_kind VARCHAR(20) NOT NULL,
    title VARCHAR(160) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL,
    playback_state VARCHAR(20) NOT NULL,
    position_seconds BIGINT NOT NULL DEFAULT 0,
    started_at DATETIME(6) NULL,
    ended_at DATETIME(6) NULL,
    last_controlled_at DATETIME(6) NULL,
    last_controlled_by BINARY(16) NULL,
    message_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_playback_sessions_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_playback_sessions_host FOREIGN KEY (host_user_id) REFERENCES users (id),
    CONSTRAINT fk_playback_sessions_message FOREIGN KEY (message_id) REFERENCES messages (id)
);

CREATE INDEX idx_playback_session_conversation_created ON playback_sessions (conversation_id, created_at);
CREATE INDEX idx_playback_session_conversation_status ON playback_sessions (conversation_id, status);

CREATE TABLE playback_session_participants (
    id BINARY(16) NOT NULL,
    session_id BINARY(16) NOT NULL,
    conversation_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role VARCHAR(20) NOT NULL,
    last_seen_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_playback_session_participant UNIQUE (session_id, user_id),
    CONSTRAINT fk_playback_participant_session FOREIGN KEY (session_id) REFERENCES playback_sessions (id),
    CONSTRAINT fk_playback_participant_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_playback_participant_user FOREIGN KEY (user_id) REFERENCES users (id)
);
