package com.example.shopify_legacy.checkout;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CheckoutCompleteRequest(
    @NotEmpty List<@Valid CheckoutLine> lines,
    @NotBlank String paymentToken
) {
}
