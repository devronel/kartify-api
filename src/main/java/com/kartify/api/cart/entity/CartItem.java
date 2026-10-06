package com.kartify.api.cart.entity;

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
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_variant_id")
  private ProductVariant productVariant;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  // Constructor
  public CartItem(){}

  // Getters & Setters
  public Long getId(){ return id; }
  public void setId(Long id) { this.id = id; }

  public Cart getCart(){ return cart; }
  public void setCart(Cart cart){ this.cart = cart; }

  public Product getProduct(){ return product; }
  public void setProduct(Product product){ this.product = product; }

  public ProductVariant getProductVariant() { return productVariant; }
  public void setProductVariant(ProductVariant productVariant) { this.productVariant = productVariant; }

  public Integer getQuantity(){ return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }

  public BigDecimal getPrice(){ return price; }
  public void setPrice(BigDecimal price){ this.price = price; }

}
