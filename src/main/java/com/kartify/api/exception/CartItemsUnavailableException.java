package com.kartify.api.exception;

import java.util.List;

import com.kartify.api.cart.dto.ItemProblem;

public class CartItemsUnavailableException extends RuntimeException {

  private final List<ItemProblem> problems;

  public CartItemsUnavailableException(List<ItemProblem> problems) {
    super("Some items in your cart are unavailable.");
    this.problems = problems;
  }

  public List<ItemProblem> getProblems() {
    return problems;
  }

}
