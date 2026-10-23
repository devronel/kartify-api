package com.kartify.api.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;
import com.kartify.api.payment.service.PaymongoService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

  private final PaymongoService paymongoService;

  public PaymentController(
    PaymongoService paymongoService
  ){
    this.paymongoService = paymongoService;
  }

  @PostMapping
  public ResponseEntity<PaymentLink> process(@RequestBody PaymentLinkRequest paymentLinkRequest){

    PaymentLink response = paymongoService.process(paymentLinkRequest);

    return ResponseEntity.ok(response);

  }

}
