package com.kartify.api.order.entity;

import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.shared.BaseEntity;
import com.kartify.api.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_status_histories")
public class OrderStatusHistory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; 

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  @Column(length = 255)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by")
  private User changeBy;

  // Constructor
  public OrderStatusHistory(){}

  // Getter & Setter
  public Long getId(){ return id; }

  public Order getOrder(){ return order; }
  public void setOrder(Order order){ this.order = order; }

  public OrderStatus getStatus(){ return status; }
  public void setStatus(OrderStatus status){ this.status = status; }

  public String getNotes(){ return notes; }
  public void setNotes(String notes){ this.notes = notes; }

  public User getChangedBy(){ return changeBy; }
  public void setChangeBy(User changeBy){ this.changeBy = changeBy; }

}
