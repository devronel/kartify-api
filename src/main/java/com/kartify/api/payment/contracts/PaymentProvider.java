package com.kartify.api.payment.contracts;

import java.util.List;

import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;

public interface PaymentProvider {

  PaymentLink process(PaymentLinkRequest data);

  void archiveLinks(List<String> paymentLinkId);

}
