CREATE TABLE product_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    extension VARCHAR(50),
    mime_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key constraint linking to products table
    CONSTRAINT fk_product_files_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Performance Indexes
CREATE INDEX idx_product_file_product ON product_files(product_id);
CREATE INDEX idx_product_file_filename ON product_files(filename);