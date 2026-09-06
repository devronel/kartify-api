CREATE TABLE product_variant_attribute_values (
    product_variant_id BIGINT NOT NULL,
    product_attribute_value_id BIGINT NOT NULL,

    PRIMARY KEY (product_variant_id, product_attribute_value_id),

    CONSTRAINT fk_pvav_variant FOREIGN KEY (product_variant_id)
        REFERENCES product_variants(id),

    CONSTRAINT fk_pvav_attribute_value FOREIGN KEY (product_attribute_value_id)
        REFERENCES product_attribute_values(id)
);