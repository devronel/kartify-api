package com.kartify.api.payment.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymongoService {

  private final String secretKey;
  private final ObjectMapper objectMapper;

  public PaymongoService(
    @Value("${paymongo.secret-key}") String secretKey,
    ObjectMapper objectMapper
  ){
    this.secretKey = Base64.getEncoder().encodeToString(
      (secretKey + ":").getBytes(StandardCharsets.UTF_8)
    );
    this.objectMapper = objectMapper;
  }

  public PaymentLink process(PaymentLinkRequest data) {
    
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
      .method("POST", HttpRequest.BodyPublishers.ofString(payload))
      .build();
  
      HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());  

      JsonNode pmResponse = objectMapper.readTree(response.body()).path("data");
      String id = pmResponse.path("id").asString();
      String checkoutUrl = pmResponse.path("url").asString();
      
      return new PaymentLink(
        id,
        checkoutUrl,
        response.body()
      );

    } catch (IOException e) {
      throw new RuntimeException("PayMongo request failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Request interrupted", e);
    }

  }  

}


/*
  BigDecimal total = new BigDecimal("1299.99");

  int amount = total.movePointRight(2).intValueExact();
*/
