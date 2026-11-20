package com.kartify.api.order.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.kartify.api.order.entity.Order;
import com.kartify.api.order.enums.OrderStatus;

public class OrderSpecifications {

  public static Specification<Order> keyword(String search){
    return (root, query, cb) -> {

      if(search == null || search.isBlank()){
        return null;
      }

      String like = "%" + search.trim().toLowerCase() + "%";

      return cb.or(
        cb.like(cb.lower(root.get("orderNumber")), like),
        cb.like(cb.lower(root.get("shippingRecipientName")), like),
        cb.like(cb.lower(root.get("shippingProvince")), like)
      );
    };
  }


  public static Specification<Order> status(String status){
    return (root, query, cb) -> {

      if(status == null || status.isBlank()){
        return null;
      }

      try {
        OrderStatus statusEnum = OrderStatus.valueOf(status.trim().toUpperCase());
        return cb.equal(root.get("status"), statusEnum);
      } catch (IllegalArgumentException e) {
        return cb.disjunction(); 
      }

    };
  }


  public static Specification<Order> dateRange(LocalDate dateFrom, LocalDate dateTo){
    return (root, query, cb) -> {

      if (dateFrom == null && dateTo == null) {
        return cb.conjunction();
      }

      if(dateFrom != null && dateTo == null){
        return cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay());
      }

      if(dateFrom == null){
        return cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay());
      }

      return cb.and(
        cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()),
        cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay())
      );

    };
  }

}
