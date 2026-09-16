package com.example.sharpoclockserver.checkout.order.payment.paypal

import com.example.sharpoclockserver.email.EmailService
import com.example.sharpoclockserver.model.OrderStatus
import com.example.sharpoclockserver.model.PaymentStatus
import com.example.sharpoclockserver.model.repository.OrderRepository
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.sdk.PaypalServerSdkClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class PaypalWebhookHandler(
    private val orderRepository: OrderRepository,
    private val paypalClient: PaypalServerSdkClient,
    private val emailService: EmailService,
    @Value($$"${app.email.from}")
    private val paymentReceiptEmail: String,
) {
    private val logger = LoggerFactory.getLogger(PaypalWebhookHandler::class.java)

    /**
     * Webhook handler: called by PayPal when payment events occur
     *
     * Flow:
     * 1. PayPal sends event to POST /checkout/payments/paypal/webhook
     * 2. Extract orderId from resource data
     * 3. Update order based on event type
     */
    fun handleWebhook(
        payload: String,
        signatureHeader: String,
    ): ResponseEntity<*> {
        return try {
            val jsonObject = JsonParser.parseString(payload).asJsonObject
            val eventType = jsonObject.get("event_type")?.asString
                ?: throw Exception("No event_type in webhook payload")

            logger.info("Received PayPal webhook event: $eventType")

            val resource = jsonObject.getAsJsonObject("resource")
                ?: throw Exception("No resource in webhook payload")

            when (eventType) {
                "PAYMENT.CAPTURE.COMPLETED" -> handlePaymentCompleted(resource)
                "PAYMENT.CAPTURE.REFUNDED" -> handlePaymentRefunded(resource)
                "PAYMENT.CAPTURE.DENIED" -> handlePaymentDenied(resource)
                "PAYMENT.CAPTURE.REVERSED" -> handlePaymentReversed(resource)
                else -> {
                    logger.info("Ignoring unhandled webhook event type: $eventType")
                    ResponseEntity.ok("Received")
                }
            }
        } catch (e: Exception) {
            logger.error("Webhook processing failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload: ${e.message}")
        }
    }

    /**
     * Payment completed: update order to PAID
     * This occurs when PayPal captures the payment
     */
    private fun handlePaymentCompleted(resource: JsonObject): ResponseEntity<*> {
        return try {
            val orderId = resource.get("custom_id")?.asString
                ?: throw Exception("No custom_id (orderId) in payment resource")

            val order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow { Exception("Order not found: $orderId") }

            if (order.paymentStatus == PaymentStatus.PAID) {
                logger.info("Order $orderId already marked as PAID, skipping")
                return ResponseEntity.ok("Already processed")
            }

            val updatedOrder = orderRepository.save(
                order.copy(
                    paymentStatus = PaymentStatus.PAID,
                    status = OrderStatus.PLACED
                )
            )

            try {
                emailService.sendPaypalPaymentReceiptEmail(
                    recipient = order.customer.email,
                    customer = order.customer,
                    orderId = orderId,
                    totalPriceCents = order.totalPriceCents
                )
                emailService.sendPaypalPaymentReceiptEmail(
                    recipient = paymentReceiptEmail,
                    customer = order.customer,
                    orderId = orderId,
                    totalPriceCents = order.totalPriceCents
                )
            } catch (e: Exception) {
                logger.warn("Failed to send receipt emails: ${e.message}")
            }

            logger.info("✅ Order $orderId payment confirmed via PayPal webhook. Status: PAID")
            ResponseEntity.ok("Payment confirmed")
        } catch (e: Exception) {
            logger.error("Error handling PAYMENT.CAPTURE.COMPLETED: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Payment denied: log it but don't change order status
     * Order stays DRAFT so customer can retry
     */
    private fun handlePaymentDenied(resource: JsonObject): ResponseEntity<*> {
        return try {
            val orderId = resource.get("custom_id")?.asString
                ?: throw Exception("No custom_id (orderId) in payment resource")
            val statusDetails = resource.getAsJsonObject("status_details")
            val reason = statusDetails?.get("reason")?.asString ?: "Unknown reason"

            logger.warn("❌ Payment denied for order $orderId: $reason")

            ResponseEntity.ok("Payment denial logged")
        } catch (e: Exception) {
            logger.error("Error handling PAYMENT.CAPTURE.DENIED: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Payment refunded: update order to REFUNDED
     * Customer requested refund or you issued one from PayPal dashboard
     */
    private fun handlePaymentRefunded(resource: JsonObject): ResponseEntity<*> {
        return try {
            val orderId = resource.get("custom_id")?.asString
                ?: throw Exception("No custom_id (orderId) in refund resource")

            val order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow { Exception("Order not found: $orderId") }

            val amountRefunded = resource.getAsJsonObject("amount")
                ?.get("value")?.asString ?: "Unknown amount"

            orderRepository.save(
                order.copy(
                    paymentStatus = PaymentStatus.REFUNDED
                )
            )

            logger.info("💰 Order $orderId refunded. Amount: $amountRefunded")
            ResponseEntity.ok("Refund processed")
        } catch (e: Exception) {
            logger.error("Error handling PAYMENT.CAPTURE.REFUNDED: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Payment reversed: typically due to chargeback or other issues
     * Flag order for review
     */
    private fun handlePaymentReversed(resource: JsonObject): ResponseEntity<*> {
        return try {
            val orderId = resource.get("custom_id")?.asString
                ?: throw Exception("No custom_id (orderId) in reversal resource")
            val statusDetails = resource.getAsJsonObject("status_details")
            val reason = statusDetails?.get("reason")?.asString ?: "Unknown reason"

            logger.error("⚠️ PAYMENT REVERSED for order $orderId - requires manual review!")
            logger.error("Reversal reason: $reason")

            val order = orderRepository.findById(UUID.fromString(orderId))
                .orElse(null)

            if (order != null) {
                try {
                    emailService.sendPaypalPaymentReversalEmail(
                        orderId = orderId,
                        customer = order.customer,
                        reversalReason = reason
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to send reversal alert email: ${e.message}")
                }
            }

            ResponseEntity.ok("Reversal noted")
        } catch (e: Exception) {
            logger.error("Error handling PAYMENT.CAPTURE.REVERSED: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }
}