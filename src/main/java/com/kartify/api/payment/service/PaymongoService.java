package com.kartify.api.payment.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.kartify.api.exception.PaymentCreationRejectedException;
import com.kartify.api.exception.PaymentCreationUnknownException;
import com.kartify.api.payment.contracts.PaymentProvider;
import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymongoService implements PaymentProvider {

  private static final Logger log = LoggerFactory.getLogger(PaymongoService.class);
  private final String secretKey;
  private final ObjectMapper objectMapper;

  public PaymongoService(@Value("${paymongo.secret-key}") String secretKey, ObjectMapper objectMapper){
    this.secretKey = Base64.getEncoder().encodeToString(
      (secretKey + ":").getBytes(StandardCharsets.UTF_8)
    );
    this.objectMapper = objectMapper;
  }


  @Override
  public PaymentLink process(PaymentLinkRequest data) {
    
    HttpResponse<String> response;

    try {

      int amount = data.amount().movePointRight(2).intValueExact();

      String payload = """
        {
          "amount": %d,
          "currency": "PHP",
          "description": "%s",
          "remarks": "%s"
        }
      """.formatted(amount, data.description(), data.remarks());
      
      HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create("https://api.paymongo.com/v1/payment_links"))
      .header("accept", "application/json")
      .header("content-type", "application/json")
      .header("authorization", "Basic " + secretKey)
      .header("Idempotency-Key", "mykey")
      .method("POST", HttpRequest.BodyPublishers.ofString(payload))
      .build();
  
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());  

    } catch (IOException e) {
      throw new RuntimeException("PayMongo request failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Request interrupted", e);
    }

    int status = response.statusCode();

    if (status == 201) {
      try {
        JsonNode pmResponse = objectMapper.readTree(response.body())
          .path("data");

        String paymentId = pmResponse.path("id").asString();
        String checkoutUrl = pmResponse.path("url").asString();

        if (paymentId.isBlank() || checkoutUrl.isBlank()) {
          throw new PaymentCreationUnknownException(
            "PayMongo returned an incomplete response."
          );
        }

        return new PaymentLink(
          paymentId,
          checkoutUrl,
          response.body()
        );

      } catch (JacksonException e) {
        // PayMongo may have created the link, but couldn't parse the response.
        throw new PaymentCreationUnknownException(
          "Unable to parse PayMongo's response.",
          e
        );
      }
    }

    if (status >= 400 && status < 500) {
      throw new PaymentCreationRejectedException(
        "PayMongo rejected the request: " + response.body()
      );
    }

    // Treat unexpected responses, especially 5xx, conservatively.
    throw new PaymentCreationUnknownException(
      "Unexpected PayMongo response: HTTP " + status
    );

  }  


  @Override
  @Async
  public void archiveLinks(List<String> paymentLinkIds){
    for (String paymentLinkId : paymentLinkIds) {
      try {
        archiveLink(paymentLinkId);
      } catch (Exception e) {
        log.warn("Failed to archive payment link {}", paymentLinkId, e);
      }
    }
  }

  
  /**
   * Archive the payment link
   * 
   * Connect to the paymongo api and use method PATCH
   * to archive the status of payment to avoid reusing it
   * 
   * @param paymentLinkId
   */
  public void archiveLink(String paymentLinkId){
    try {
      String link = "https://api.paymongo.com/v1/payment_links/" + paymentLinkId;
  
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(link))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header("authorization", "Basic " + secretKey)
        .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"archive\":true}"))
        .build();
  
      HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  
    } catch (IOException e) {
      throw new RuntimeException("PayMongo request failed", e);
    } catch (InterruptedException e){
      throw new RuntimeException("Request interrupted", e);
    }
  }

}


/*
  BigDecimal total = new BigDecimal("1299.99");

  int amount = total.movePointRight(2).intValueExact();
*/
