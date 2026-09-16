package com.example.sharpoclockserver.checkout.order.payment.paypal

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.jetbrains.annotations.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

// POST https://localhost:8080/checkout/payments/paypal/{orderId}/create-intent: creates a PayPal Payment Intent
// POST https://localhost:8080/checkout/payments/paypal/{orderId}/confirm: confirms PayPal payment
// POST https://localhost:8080/checkout/payments/paypal/{orderId}/webhook: handles PayPal's webhook updates (refund, cancellation, failure, approval, dispute)

@RestController
@RequestMapping("/checkout/payments/paypal")
class PaypalController(
    private val paypalService: PaypalService,
    private val paypalWebhookHandler: PaypalWebhookHandler
) {

    data class PaymentIntentResponse(
        val orderId: String,
        val paypalOrderId: String,
        val approvalUrl: String,
    )

    data class ConfirmRequest(
        @NotNull
        @NotBlank
        val paypalOrderId: String,
    )

    data class ConfirmResponse(
        val orderId: String,
        val orderStatus: String,
        val paymentStatus: String,
        val paypalPaymentStatus: String,
    )

    @PostMapping("/{orderId}/create-intent")
    fun createPaymentIntent(
        @PathVariable("orderId") orderId: String,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> = paypalService.createPaymentIntent(
        orderId = orderId,
        customerId = jwt.subject
    )

    @PostMapping("/{orderId}/confirm")
    fun confirmPayment(
        @PathVariable("orderId") orderId: String,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid confirmRequest: ConfirmRequest,
    ): ResponseEntity<*> = paypalService.confirmPayment(
        orderId = orderId,
        customerId = jwt.subject,
        confirmRequest = confirmRequest
    )

    @PostMapping("/webhook")
    fun webhookHandler(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signatureHeader: String,
    ): ResponseEntity<*> = paypalWebhookHandler.handleWebhook(
        payload = payload,
        signatureHeader = signatureHeader
    )
}