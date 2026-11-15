package com.kartify.api.payment.contracts;

import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;

public interface PaymentProvider {

  PaymentLink process(PaymentLinkRequest data);

}
