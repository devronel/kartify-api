package com.kartify.api.cart.entity;

import java.util.ArrayList;
import java.util.List;

import com.kartify.api.cart.enums.CartStatus;
import com.kartify.api.shared.BaseEntity;
import com.kartify.api.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToMany(
    mappedBy = "cart", 
    fetch = FetchType.LAZY, 
    cascade = CascadeType.ALL, 
    orphanRemoval = true
  )
  private List<CartItem> items = new ArrayList<>();

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private CartStatus status;

  // Contructor
  public Cart () {}

  // Getter & Setter
  public Long getId() { return id; }

  public List<CartItem> getItems(){ return items; }
  public void addItem(CartItem item){
    items.add(item);
    item.setCart(this);
  }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public CartStatus getStatus() { return status; }
  public void setStatus(CartStatus status) { this.status = status; }

}
