package com.example.sharpoclockserver.checkout.order.payment.stripe

import jakarta.validation.constraints.NotBlank
import org.jetbrains.annotations.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

// POST https://localhost:8080/checkout/payments/stripe/{orderId}/create-intent: creates a Stripe Payment Intent
// POST https://localhost:8080/checkout/payments/stripe/{orderId}/confirm: confirms Stripe payment
// POST https://localhost:8080/checkout/payments/stripe/{orderId}/webhook: handles Stripe's webhook updates (refund, cancellation, failure)

@RestController
@RequestMapping("/checkout/payments/stripe")
class StripeController(
    private val stripeService: StripeService,
    private val stripeWebhookHandler: StripeWebhookHandler,
) {

    data class PaymentIntentResponse(
        val orderId: String,
        val paymentIntentId: String,
        val clientSecret: String,
    )

    data class ConfirmRequest(
        @NotNull
        @NotBlank
        val paymentIntentId: String,
    )

    data class ConfirmResponse(
        val orderId: String,
        val orderStatus: String,
        val intentStatus: String,
        val paymentStatus: String,
    )

    @PostMapping("/{orderId}/create-intent}")
    fun createPaymentIntent(
        @PathVariable orderId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> = stripeService.createPaymentIntent(
        orderId = orderId,
        customerId = jwt.subject
    )

    @PostMapping("/{orderId}/confirm")
    fun confirmPayment(
        @PathVariable orderId: String,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody confirmRequest: ConfirmRequest,
    ): ResponseEntity<*> = stripeService.confirmPayment(
        orderId = orderId,
        customerId = jwt.subject,
        confirmRequest = confirmRequest
    )

    @PostMapping("/webhook")
    fun webhookHandler(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signatureHeader: String,
    ): ResponseEntity<*> = stripeWebhookHandler.handleWebhook(payload, signatureHeader)
}