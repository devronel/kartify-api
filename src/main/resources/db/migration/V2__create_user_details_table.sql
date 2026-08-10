CREATE TABLE user_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(30) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign Key constraint linking back to users(id)
    CONSTRAINT fk_user_details_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);