package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.repository.CartItemRepository;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.PaymentCreationRejectedException;
import com.kartify.api.exception.PaymentCreationUnknownException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.dto.AddressInformationResponse;
import com.kartify.api.order.dto.CheckoutCartItemResponse;
import com.kartify.api.order.dto.CheckoutDetailsResponse;
import com.kartify.api.order.dto.CheckoutResponse;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderPaymentResponse;
import com.kartify.api.order.dto.PaymentAttemptResponse;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;
import com.kartify.api.payment.service.PaymongoService;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.repository.UserAddressRepository;

@Service
public class CheckoutService {

  private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
  
  private final PaymongoService paymongoService;
  private final PaymentService paymentService;
  private final UserAddressRepository userAddressRepository;
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final FileStorage fileStorage;


  public CheckoutService(
    PaymongoService paymongoService,
    PaymentService paymentService,
    UserAddressRepository userAddressRepository,
    CartRepository cartRepository,
    CartItemRepository cartItemRepository,
    FileStorage fileStorage
  ){
    this.paymongoService = paymongoService;
    this.paymentService = paymentService;
    this.userAddressRepository = userAddressRepository;
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.fileStorage = fileStorage;
  }


  // Process and Pay the Order
  public CheckoutResponse placeOrder(Long userId,  OrderCreateRequest payload){

    OrderPaymentResponse order = paymentService.createOrderAndPayment(userId, payload);

    try {

      PaymentLink paymentProvider = paymongoService.process(new PaymentLinkRequest(
        order.idempotencyKey(),
        order.totalAmount(),
        "Payment for Order " + order.orderNumber(),
        "Order Number: " + order.orderNumber()
      ));
  
      paymentService.update(
        order.paymentId(), 
        order.orderId(), 
        paymentProvider.paymentLinkId(), 
        paymentProvider.checkoutUrl(), 
        paymentProvider.payload()
      );
  
      return new CheckoutResponse(
        paymentProvider.paymentLinkId(), 
        paymentProvider.checkoutUrl()
      );
      
    } catch (PaymentCreationRejectedException e) {
      paymentService.updateStatus(order.paymentId(), order.orderId(), PaymentStatus.SETUP_FAILED);
      throw e;
    } catch (PaymentCreationUnknownException e) {
      paymentService.updateStatus(order.paymentId(), order.orderId(), PaymentStatus.CREATION_UNKNOWN);
      throw e;
    }

  }


  // Retry Payment
  public CheckoutResponse retryPayment(Long userId, String orderNumber){

    PaymentAttemptResponse attemp = paymentService.prepareRetry(userId, orderNumber);

    try {

      PaymentLink paymentProvider = paymongoService.process(new PaymentLinkRequest(
        attemp.idempotencyKey(),
        attemp.totalAmount(),
        "Payment for Order " + attemp.orderNumber(),
        "Order Number: " + attemp.orderNumber()
      ));

      paymentService.completePaymentSetup(
        attemp.paymentId(), 
        paymentProvider.paymentLinkId(), 
        paymentProvider.checkoutUrl(), 
        paymentProvider.payload()
      );

      return new CheckoutResponse(
        paymentProvider.paymentLinkId(),
        paymentProvider.checkoutUrl()
      );

    } catch (PaymentCreationRejectedException e) {
      paymentService.updateStatus(attemp.paymentId(), attemp.orderId(), PaymentStatus.SETUP_FAILED);
      throw e;
    } catch (PaymentCreationUnknownException e) {
      paymentService.updateStatus(attemp.paymentId(), attemp.orderId(), PaymentStatus.CREATION_UNKNOWN);
      throw e;
    }

  }


  // Get checkout information
  public CheckoutDetailsResponse getDetail(Long userId){

    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Cart not found."));

      List<CartItem> cartItems = cart.getItems();

    
    if(cartItems.isEmpty()){
      throw new ResourceNotFoundException("No cart item available.");
    }


    List<CheckoutCartItemResponse> items = cartItems.stream()
      .map(item -> new CheckoutCartItemResponse(
        item.getId(),
        fileStorage.getUrl("files/public/product/images/" + item.getProduct().getPrimaryImage()),
        item.getProduct().getName(),
        item.getProductVariant() != null ? item.getProductVariant().getAttributeValueName() : null,
        item.getQuantity(),
        item.getPrice()
      ))
      .toList();

    List<UserAddress> addresses = userAddressRepository.findByUserId(userId);

    List<AddressInformationResponse> userAddress = addresses.stream()
      .map(address -> new AddressInformationResponse(
        address.getId(),
        address.getLabel(),
        address.getRecipientName(),
        address.getPhone(),
        address.getAddressLine1(),
        address.getAddressLine2(),
        address.getBarangay(),
        address.getCity(),
        address.getProvince(),
        address.getRegion(),
        address.getPostalCode(),
        address.getCountry(),
        address.getIsDefault()
      ))
      .toList();

    // Calculate the the total item in the cart
    BigDecimal subtotal = cartItemSubtotal(cartItems);
    BigDecimal shippingFee = BigDecimal.ZERO;
    BigDecimal discountAmount = BigDecimal.ZERO;
    BigDecimal total = subtotal.add(shippingFee).subtract(discountAmount);

    return new CheckoutDetailsResponse(
      userAddress,
      items,
      subtotal,
      total
    );

  }


  // Calculate cart item subtotal
  private BigDecimal cartItemSubtotal(List<CartItem> cartItems){

    return cartItems.stream()
      .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, (accumulator, next) -> accumulator.add(next)); 

  }
}
