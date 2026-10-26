package com.kartify.api.payment.controller;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.order.service.PaymentService;
import com.kartify.api.payment.service.PaymongoEventParser;
import com.kartify.api.payment.service.PaymongoWebhookVerifier;

@RestController
@RequestMapping("/api/webhooks")
public class PaymongoWebhookController {

  private static final Logger log = LoggerFactory.getLogger(PaymongoWebhookController.class);

  private final PaymongoWebhookVerifier verifier;
  private final PaymongoEventParser parser;
  private final PaymentService paymentService;

  public PaymongoWebhookController(
    PaymongoWebhookVerifier verifier,
    PaymongoEventParser parser,
    PaymentService paymentService
  ) {
    this.verifier = verifier;
    this.parser = parser;
    this.paymentService = paymentService;
  }

  @PostMapping("/paymongo")
  public ResponseEntity<Void> handle(@RequestBody String rawBody, @RequestHeader(value = "Paymongo-Signature", required = false) String signature) {

    if (!verifier.isValid(rawBody, signature)) {
      log.warn("PayMongo webhook invalid: {}", rawBody);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    parser.parseLinkPaid(rawBody).ifPresent(event -> {
      paymentService.markPaid(
        event.linkdId(), 
        BigDecimal.valueOf(event.amountCentavos(), 2), 
        event.paidAt(), 
        rawBody
      );
    });

    log.info("PayMongo webhook received: {}", rawBody);   // temporary
    return ResponseEntity.ok().build();
  }

}
