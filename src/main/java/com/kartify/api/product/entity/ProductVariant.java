package com.kartify.api.product.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.kartify.api.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "product_variant_attribute_values",
        joinColumns = @JoinColumn(name = "product_variant_id"),
        inverseJoinColumns = @JoinColumn(name = "product_attribute_value_id")
    )
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100, nullable = false, unique = true)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // --- Constructor ---
    public ProductVariant(){}

    // --- Getter & Setter ---
    public Long getId(){ return id; }

    public List<ProductAttributeValue> getAttributeValues(){ return attributeValues; }
    public void addVariantAttributeValue(ProductAttributeValue attributeValue){
        attributeValues.add(attributeValue);
        attributeValue.getProductVariants().add(this);
    }
    public void removeVariantAttributeValue(ProductAttributeValue attributeValue){
        attributeValues.remove(attributeValue);
        attributeValue.getProductVariants().remove(this);
    }

    public Product getProduct(){ return product; }
    public void setProduct(Product product){ this.product = product; }

    public String getSku(){ return sku; }
    public void setSku(String sku){ this.sku = sku; }

    public BigDecimal getPrice(){ return price; }
    public void setPrice(BigDecimal price){ this.price = price; }

    public Integer getStockQuantity(){ return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity){ this.stockQuantity = stockQuantity; }

    public Boolean getIsActive(){ return isActive; }
    public void setIsActive(Boolean isActive){ this.isActive = isActive; }
}
