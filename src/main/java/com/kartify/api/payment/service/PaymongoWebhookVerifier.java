package com.kartify.api.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymongoWebhookVerifier {

  // t=1793013065,te=5070bc2e2ed55353368c6ed252bcf99337ca0aeb173a2123ccf192393e6592fa,li=

  private static final Logger log = LoggerFactory.getLogger(PaymongoWebhookVerifier.class);

  @Value("${paymongo.webhook-secret-key}")
  private String secret;

  @Value("${paymongo.livemode}")
  private boolean livemode;

  public boolean isValid(String rawBody, String header) {

    if (header == null || header.isBlank()) return false;

    log.info(header);

    String timestamp = null;
    String te = null;
    String li = null;

    for (String part : header.split(",")) {
      String[] kv = part.trim().split("=", 2);
      if (kv.length != 2) continue;
      switch (kv[0]) {
        case "t"  -> timestamp = kv[1];
        case "te" -> te = kv[1];
        case "li" -> li = kv[1];
      }
    }

    String expected = livemode ? li : te;

    if (timestamp == null || expected == null || expected.isBlank()) return false;

    String computed = hmacSha256Hex(secret, timestamp + "." + rawBody);
    
    return MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));

  }

  private String hmacSha256Hex(String key, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

}
