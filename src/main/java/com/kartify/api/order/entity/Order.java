package com.kartify.api.order.entity;

import java.math.BigDecimal;

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
@Table(name = "orders")
public class Order extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;  

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "order_number", nullable = false, unique = true, length = 50)
  private String orderNumber;

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
  private BigDecimal shippingFee;

  @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal discountAmount;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal total;

  @Column(name = "shipping_recipient_name", nullable = false, length = 255)
  private String shippingRecipientName;

  @Column(name = "shipping_phone", nullable = false, length = 30)
  private String shippingPhone;

  @Column(name = "shipping_address_line_1", nullable = false, length = 255)
  private String shippingAddressLine1;

  @Column(name = "shipping_address_line_2", length = 255)
  private String shippingAddressLine2;

  @Column(name = "shipping_barangay", nullable = false, length = 100)
  private String shippingBarangay;

  @Column(name = "shipping_city", nullable = false, length = 100)
  private String shippingCity;

  @Column(name = "shipping_province", nullable = false, length = 100)
  private String shippingProvince;

  @Column(name = "shipping_region", nullable = false, length = 100)
  private String shippingRegion;

  @Column(name = "shipping_postal_code", nullable = false, length = 20)
  private String shippingPostalCode;

  @Column(name = "shipping_country", nullable = false, length = 100)
  private String shippingCountry;

  private String notes;

  // Constructor
  public Order(){}

  // Getter & Setter
  public Long getId(){ return id; }

  public User getUser(){ return user; }
  public void setUser(User user){ this.user = user; }

  public String getOrderNumber(){ return orderNumber; }
  public void setOrderNumber(String orderNumber){ this.orderNumber = orderNumber; }

  public OrderStatus getStatus(){ return status; }
  public void setStatus(OrderStatus status){ this.status = status; }

  public BigDecimal getSubtotal(){ return subtotal; }
  public void setSubtotal(BigDecimal subtotal){ this.subtotal = subtotal; }

  public BigDecimal getShippingFee(){ return shippingFee; }
  public void setShippingFee(BigDecimal shippingFee){ this.shippingFee = shippingFee; }

  public BigDecimal getDiscountAmount(){ return discountAmount; }
  public void setDiscountAmount(BigDecimal discountAmount){ this.discountAmount = discountAmount; }

  public BigDecimal getTotal(){ return total; }
  public void setTotal(BigDecimal total){ this.total = total; }

  public String getShippingRecipientName(){ return shippingRecipientName; }
  public void setShippingRecipientName(String shippingRecipientName){ this.shippingRecipientName = shippingRecipientName; }

  public String getShippingPhone(){ return shippingPhone; }
  public void setShippingPhone(String shippingPhone){ this.shippingPhone = shippingPhone; }

  public String getShippingAddressLine1(){ return shippingAddressLine1; }
  public void setShippingAddressLine1(String shippingAddressLine1){ this.shippingAddressLine1 = shippingAddressLine1; }

  public String getShippingAddressLine2(){ return shippingAddressLine2; }
  public void setShippingAddressLine2(String shippingAddressLine2){ this.shippingAddressLine2 = shippingAddressLine2; }

  public String getShippingBarangay(){ return shippingBarangay; }
  public void setShippingBarangay(String shippingBarangay){ this.shippingBarangay = shippingBarangay; }

  public String getShippingCity(){ return shippingCity; }
  public void setShippingCity(String shippingCity){ this.shippingCity = shippingCity; }

  public String getShippingProvince(){ return shippingProvince; }
  public void setShippingProvince(String shippingProvince){ this.shippingProvince = shippingProvince; }

  public String getShippingRegion(){ return shippingRegion; }
  public void setShippingRegion(String shippingRegion){ this.shippingRegion = shippingRegion; }

  public String getShippingPostalCode(){ return shippingPostalCode; }
  public void setShippingPostalCode(String shippingPostalCode){ this.shippingPostalCode = shippingPostalCode; }

  public String getShippingCountry(){ return shippingCountry; }
  public void setShippingCountry(String shippingCountry){ this.shippingCountry = shippingCountry; }

  public String getNotes(){ return notes; }
  public void setNotes(String notes){ this.notes = notes; }
  
}
