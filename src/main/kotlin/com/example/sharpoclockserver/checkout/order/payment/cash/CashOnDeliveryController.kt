package com.example.sharpoclockserver.checkout.order.payment.cash

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

// POST https://localhost:8080/checkout/payments/cash/{orderId}/create-payment: creates a COD payment record
// POST https://localhost:8080/checkout/payments/cash/{orderId}/confirm: ADMIN confirms payment after delivery agency notification
// POST https://localhost:8080/checkout/payments/cash/{orderId}/refund: customer requests refund within refund window
// POST https://localhost:8080/checkout/payments/cash/{orderId}/cancel: customer cancels COD payment

@RestController
@RequestMapping("/checkout/payments/cash")
class CashOnDeliveryController(
    private val cashOnDeliveryService: CashOnDeliveryService
) {

    data class PaymentResponse(
        val orderId: String,
        val paymentId: String,
        val paymentMethod: String,
        val paymentStatus: String,
        val amountInCents: Long
    )

    data class RefundRequest(
        @NotBlank
        val orderLineId: String,
    )

    data class RefundResponse(
        val orderId: String,
        val orderLineId: String,
        val paymentMethod: String,
        val paymentStatus: String,
        val refundAmountInCents: Long,
        val orderTotalAmountInCents: Long,
    )

    @PostMapping("/{orderId}/create-payment")
    fun createPayment(
        @PathVariable orderId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<*> = cashOnDeliveryService.createPayment(
        orderId = orderId,
        customerId = jwt.subject
    )

    @PostMapping("/{orderId}/confirm")
    fun confirmPayment(
        @PathVariable orderId: String,
        @RequestParam customerId: String,
    ): ResponseEntity<*> = cashOnDeliveryService.confirmPayment(
        orderId = orderId,
        customerId = customerId
    )

    @PostMapping("/{orderId}/refund")
    fun refundPayment(
        @PathVariable orderId: String,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid refundRequest: RefundRequest
    ): ResponseEntity<*> = cashOnDeliveryService.refund(
        orderId = orderId,
        customerId = jwt.subject,
        refundRequest = refundRequest
    )

    @PostMapping("/{orderId}/cancel")
    fun cancelPayment(
        @PathVariable orderId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<*> = cashOnDeliveryService.cancel(
        orderId = orderId,
        customerId = jwt.subject
    )
}