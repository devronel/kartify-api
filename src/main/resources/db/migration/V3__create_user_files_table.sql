CREATE TABLE user_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    extension VARCHAR(50),
    mime_type VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key constraint linking to users table
    CONSTRAINT fk_user_files_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Performance Indexes
CREATE INDEX idx_user_file_user ON user_files(user_id);
CREATE INDEX idx_user_file_filename ON user_files(filename);