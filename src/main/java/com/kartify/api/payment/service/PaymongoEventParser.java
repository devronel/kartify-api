package com.kartify.api.payment.service;

import java.lang.classfile.ClassFile.Option;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kartify.api.payment.dto.LinkPaidEvent;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymongoEventParser {

  private final ObjectMapper objectMapper;

  public PaymongoEventParser(ObjectMapper objectMapper){
    this.objectMapper = objectMapper;
  }

  public Optional<LinkPaidEvent> parseLinkPaid(String rawBody){

    try {
      
      JsonNode event = objectMapper.readTree(rawBody).path("data").path("attributes");

      if(!"link.payment.paid".equals(event.path("type").asString())){
        return Optional.empty();
      }

      JsonNode link = event.path("data");
      JsonNode attrs = link.path("attributes");

      String linkId= link.path("id").asString();
      if(linkId.isBlank() || !"paid".equals(attrs.path("status").asString())){
        return Optional.empty();
      }

      int amount = attrs.path("amount").asInt();

      long paidAtEpoch = attrs.path("payments").path(0)
        .path("data")
        .path("attributes")
        .path("paid_at")
        .asLong(0);

      Instant paidAt = paidAtEpoch > 0 ? Instant.ofEpochSecond(paidAtEpoch) : Instant.now();

      return Optional.of(new LinkPaidEvent(linkId, amount, paidAt));

    } catch (JacksonException e) {
      
      return Optional.empty();

    }

  }

}
