package com.example.shopify_legacy.checkout;

public record CheckoutCompleteResponse(
    Long orderId,
    String status
) {    
}
