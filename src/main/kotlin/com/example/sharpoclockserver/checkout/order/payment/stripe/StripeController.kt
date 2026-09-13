package com.example.sharpoclockserver.checkout.order.payment.stripe

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/checkout/payments/stripe")
class StripeController(
    private val stripeService: StripeService,
) {
}