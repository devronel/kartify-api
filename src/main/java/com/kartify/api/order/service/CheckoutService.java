package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
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
import com.kartify.api.payment.contracts.PaymentProvider;
import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.repository.UserAddressRepository;

@Service
public class CheckoutService {

  private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
  
  // private final PaymongoService paymongoService;
  private final PaymentProvider paymentProvider;
  private final PaymentService paymentService;
  private final UserAddressRepository userAddressRepository;
  private final CartRepository cartRepository;
  private final FileStorage fileStorage;


  public CheckoutService(
    PaymentProvider paymentProvider,
    PaymentService paymentService,
    UserAddressRepository userAddressRepository,
    CartRepository cartRepository,
    FileStorage fileStorage
  ){
    this.paymentProvider = paymentProvider;
    this.paymentService = paymentService;
    this.userAddressRepository = userAddressRepository;
    this.cartRepository = cartRepository;
    this.fileStorage = fileStorage;
  }


  /**
   * Places an order and initiates payment through the payment provider.
   *
   * Creates the order and payment record first, then requests a payment link
   * from the provider. Updates the payment record based on the result of the
   * payment creation request.
   *
   * @param userId
   * @param payload contains order details
   * @return the checkout response containing the order number and payment URL
   * @throws PaymentCreationRejectedException if the payment provider rejects the request
   * @throws PaymentCreationUnknownException if the payment creation result cannot be determined
   */
  public CheckoutResponse placeOrder(Long userId,  OrderCreateRequest payload){

    OrderPaymentResponse order = paymentService.createOrderAndPayment(userId, payload);

    try {

      PaymentLink paymentProv = paymentProvider.process(new PaymentLinkRequest(
        order.idempotencyKey(),
        order.totalAmount(),
        "Payment for Order " + order.orderNumber(),
        "Order Number: " + order.orderNumber()
      ));
  
      paymentService.update(
        order.paymentId(), 
        order.orderId(), 
        paymentProv.paymentLinkId(), 
        paymentProv.checkoutUrl(), 
        paymentProv.payload()
      );
  
      return new CheckoutResponse(
        order.orderNumber(), 
        paymentProv.checkoutUrl()
      );
      
    } catch (PaymentCreationRejectedException e) {
      // The provider explicitly rejected the payment creation request.
      paymentService.updateStatus(order.paymentId(), order.orderId(), PaymentStatus.SETUP_FAILED);
      throw e;
    } catch (PaymentCreationUnknownException e) {
      // The provider's response is uncertain, so the same idempotency key can be reused.
      paymentService.updateStatus(order.paymentId(), order.orderId(), PaymentStatus.CREATION_UNKNOWN);
      throw e;
    }

  }


  /**
   * Retries payment creation for an existing order.
   *
   * Prepares a new payment attempt or reuses the existing attempt when its
   * creation result is unknown. Requests a payment link from the provider
   * and updates the payment record based on the result.
   *
   * @param userId
   * @param orderNumber the order number to retry payment for
   * @return the checkout response containing the order number and payment URL
   * @throws PaymentCreationRejectedException if the payment provider rejects the request
   * @throws PaymentCreationUnknownException if the payment creation result cannot be determined
   */
  public CheckoutResponse retryPayment(Long userId, String orderNumber){

    PaymentAttemptResponse attemp = paymentService.prepareRetry(userId, orderNumber);

    try {

      PaymentLink paymentProv = paymentProvider.process(new PaymentLinkRequest(
        attemp.idempotencyKey(),
        attemp.totalAmount(),
        "Payment for Order " + attemp.orderNumber(),
        "Order Number: " + attemp.orderNumber()
      ));

      paymentService.completePaymentSetup(
        attemp.paymentId(), 
        paymentProv.paymentLinkId(), 
        paymentProv.checkoutUrl(), 
        paymentProv.payload()
      );

      return new CheckoutResponse(
        attemp.orderNumber(),
        paymentProv.checkoutUrl()
      );

    } catch (PaymentCreationRejectedException e) {
      // The provider explicitly rejected the payment creation request.
      paymentService.updateStatus(attemp.paymentId(), attemp.orderId(), PaymentStatus.SETUP_FAILED);
      throw e;
    } catch (PaymentCreationUnknownException e) {
      // The provider's response is uncertain, so the same idempotency key can be reused.
      paymentService.updateStatus(attemp.paymentId(), attemp.orderId(), PaymentStatus.CREATION_UNKNOWN);
      throw e;
    }

  }


  /**
   * Retrieves the information required for the checkout page.
   *
   * Includes the user's cart items, saved addresses, and order subtotal
   * and total. Shipping fees and discounts are currently set to zero.
   *
   * @param userId
   * @return the checkout details containing cart items, addresses, and order totals
   * @throws ResourceNotFoundException if the user's cart does not exist or contains no items
  */
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
