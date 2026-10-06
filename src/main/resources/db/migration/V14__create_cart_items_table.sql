CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_variant_id BIGINT,
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_cart_items_product_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE INDEX idx_c_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_c_items_product_id ON cart_items(product_id);
CREATE INDEX idx_c_items_product_variant_id ON cart_items(product_variant_id);