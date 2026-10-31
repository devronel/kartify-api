package com.kartify.api.order.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.enums.PaymentStatus;

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
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; 

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "payment_method", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  @Column(name = "provider_payment_id", length = 255, unique = true)
  private String providerPaymentId;

  @Column(name = "checkout_url", length = 1000)
  private String checkoutUrl;

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  private String payload;

  @Column(name = "paid_at")
  private Instant paidAt;

  // Constructor
  public Payment(){}

  // Getter & Setter
  public Long getId(){ return id; }

  public Order getOrder(){ return order; }
  public void setOrder(Order order){ this.order = order; }

  public String getProvider(){ return provider; }
  public void setProvider(String provider){ this.provider = provider; }

  public PaymentMethod getPaymentMethod(){ return paymentMethod; }
  public void setPaymentMethod(PaymentMethod paymentMethod){ this.paymentMethod = paymentMethod; }

  public String getProviderPaymentId(){ return providerPaymentId; }
  public void setProviderPaymentId(String providerPaymentId){ this.providerPaymentId = providerPaymentId; }

  public String getCheckoutUrl(){ return checkoutUrl; }
  public void setCheckoutUrl(String checkoutUrl){ this.checkoutUrl = checkoutUrl; }

  public PaymentStatus getStatus(){ return status; }
  public void setStatus(PaymentStatus status){ this.status = status; }

  public BigDecimal getAmount(){ return amount; }
  public void setAmount(BigDecimal amount){ this.amount = amount; }

  public String getCurrency(){ return currency; }
  public void setCurrency(String currency){ this.currency = currency; }

  public String getPayload(){ return payload; }
  public void setPayload(String payload){ this.payload = payload; }

  public Instant getPaidAt(){ return paidAt; }
  public void setPaidAt(Instant paidAt){ this.paidAt = paidAt; }

}
