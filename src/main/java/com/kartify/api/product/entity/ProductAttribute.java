package com.kartify.api.product.entity;

import java.util.ArrayList;
import java.util.List;

import com.kartify.api.shared.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_attributes")
public class ProductAttribute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(
        mappedBy = "productAttribute", 
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @Column(length = 100, nullable = false)
    private String name;

    // --- Constructor ---
    public ProductAttribute(){}

    // --- Getter & Setter ---
    public Long getId(){ return id; }

    public List<ProductAttributeValue> getAttributeValues(){ return attributeValues; }
    public void addAttributeValue(ProductAttributeValue attributeValue){
        attributeValues.add(attributeValue);
        attributeValue.setProductAttribute(this);
    }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }

}
