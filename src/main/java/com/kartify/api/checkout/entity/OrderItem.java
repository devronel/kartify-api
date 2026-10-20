package com.kartify.api.checkout.entity;

import java.math.BigDecimal;

import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
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
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; 
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_variant_id")
  private ProductVariant productVariant;

  @Column(name = "product_name", nullable = false, length = 255)
  private String productName;

  @Column(name = "variant_label", length = 255)
  private String variantLabel;

  @Column(length = 100)
  private String sku;

  @Column(name = "product_image", length = 255)
  private String productImage;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
  private BigDecimal lineTotal;

  // Constructor
  public OrderItem(){}

  // Getter & Setter
  public Long getId(){ return id; }

  public Order getOrder(){ return order; }
  public void setOrder(Order order){ this.order = order; }

  public Product getProduct(){ return product; }
  public void setProduct(Product product){ this.product = product; }

  public ProductVariant getProductVariant(){ return productVariant; }
  public void setProductVariant(ProductVariant productVariant){ this.productVariant = productVariant; }

  public String getProductName(){ return productName; }
  public void setProductName(String productName){ this.productName = productName; }

  public String getVariantLabel(){ return variantLabel; }
  public void setVariantLabel(String variantLabel){ this.variantLabel = variantLabel; }

  public String getSku(){ return sku; }
  public void setSku(String sku){ this.sku = sku; }

  public String getProductImage(){ return productImage; }
  public void setProductImage(String productImage){ this.productImage = productImage; }

  public BigDecimal getPrice(){ return price; }
  public void setPrice(BigDecimal price){ this.price = price; }

  public Integer getQuantity(){ return quantity; }
  public void setQuantity(Integer quantity){ this.quantity = quantity; }

  public BigDecimal getLineTotal(){ return lineTotal; }
  public void setLineTotal(BigDecimal lineTotal){ this.lineTotal = lineTotal; }

}
