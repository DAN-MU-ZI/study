package com.example.shopify_legacy.checkout;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping("/checkouts")
public class CheckoutController {

	private final CheckoutService checkoutService;

	@PostMapping("/{checkoutId}/complete")
	public CheckoutCompleteResponse  postMethodName(
		@PathVariable("checkoutId") Long checkoutId,
		@RequestBody CheckoutCompleteRequest request

	) {
		return checkoutService.complete(checkoutId, request);
	}
	
}
