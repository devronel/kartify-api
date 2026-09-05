package com.kartify.api.product.entity;

import com.kartify.api.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_attribute_values")
public class ProductAttributeValue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_attribute_id", nullable = false)
    private ProductAttribute productAttribute;

    @Column(length = 100, nullable = false)
    private String value;

    // --- Constructor ---
    public ProductAttributeValue(){}

    // --- Getter & Setter ---
    public Long getId(){ return id; }

    public ProductAttribute getProductAttribute(){ return productAttribute; }
    public void setProductAttribute(ProductAttribute productAttribute){ this.productAttribute = productAttribute; }

    public String getValue(){ return value; }
    public void setValue(String value){ this.value = value; }

}
