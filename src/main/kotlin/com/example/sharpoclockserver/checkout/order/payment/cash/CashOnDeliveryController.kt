package com.example.sharpoclockserver.checkout.order.payment.cash

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

// POST https://localhost:8080/checkout/payments/cash/{orderId}/create-payment: updates the order's "paymentStatus" and "cashOnDeliveryPaymentId"
// POST https://localhost:8080/checkout/payments/cash/{orderId}/cancel: updates the order's "paymentStatus" above and closes the order
// POST https://localhost:8080/checkout/payments/cash/{orderId}/refund: updates the order's "paymentStatus" and closes the order, it can only be called during the refund duration
/**
 * ADMIN ONLY
 * */
// POST https://localhost:8080/checkout/payments/cash/{orderId}/confirm: updates the order's "paymentStatus" above then closes the order after the end of the refund duration

@RestController
@RequestMapping("/checkout/payments/cash")
class CashOnDeliveryController(
    private val cashOnDeliveryService: CashOnDeliveryService
) {

    data class ConfirmRequest(
        val customerId: String,

        val adminIdentifier: String,
        val adminPassword: String,
    )

    data class PaymentResponse(
        val orderId: String,

        val paymentId: String,
        val paymentMethod: String,
        val paymentStatus: String,

        val amountInCents: Long
    )

    data class RefundRequest(
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
        confirmRequest: ConfirmRequest,
    ): ResponseEntity<*> = cashOnDeliveryService.confirmPayment(
        orderId = orderId,
        confirmRequest = confirmRequest
    )

    @PostMapping("/{orderId}/refund")
    fun refundPayment(
        @PathVariable orderId: String,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody refundRequest: RefundRequest
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