CREATE TABLE product_attribute_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_attribute_id BIGINT NOT NULL,
    value VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_attribute_values_attribute FOREIGN KEY (product_attribute_id)
        REFERENCES product_attributes (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_attribute_values_attribute ON product_attribute_values(product_attribute_id);