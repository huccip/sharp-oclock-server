package com.example.sharpoclockserver.checkout.order.payment.stripe

import com.example.sharpoclockserver.email.EmailService
import com.example.sharpoclockserver.model.OrderStatus
import com.example.sharpoclockserver.model.PaymentStatus
import com.example.sharpoclockserver.model.repository.OrderRepository
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stripe.net.Webhook
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class StripeWebhookHandler(
    private val orderRepository: OrderRepository,
    @Value($$"${stripe.webhook.secret}")
    private val webhookSecret: String,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(StripeWebhookHandler::class.java)

    /**
     * Main webhook handler: called by Stripe when payment events occur
     * 
     * Flow:
     * 1. Stripe sends event to POST /checkout/payments/stripe/webhook
     * 2. We verify signature (prevents hackers from faking events)
     * 3. Extract orderId from metadata
     * 4. Update order based on event type
     */
    fun handleWebhook(
        payload: String,
        signatureHeader: String,
    ): ResponseEntity<*> {
        return try {
            val event = Webhook.constructEvent(payload, signatureHeader, webhookSecret)

            logger.info("Received Stripe webhook event: ${event.type} (id: ${event.id})")

            val jsonObject = JsonParser.parseString(payload).asJsonObject
            val dataObject = jsonObject.getAsJsonObject("data").getAsJsonObject("object")

            when (event.type) {
                "charge.succeeded" -> handleChargeSucceeded(dataObject)
                "charge.refunded" -> handleChargeRefunded(dataObject)
                "charge.dispute.created" -> handleChargeDispute(dataObject)
                "payment_intent.succeeded" -> handlePaymentIntentSucceeded(dataObject)
                "payment_intent.payment_failed" -> handlePaymentFailed(dataObject)
                else -> {
                    logger.info("Ignoring unhandled webhook event type: ${event.type}")
                    ResponseEntity.ok("Received")
                }
            }
        } catch (e: Exception) {
            logger.error("Webhook verification failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature")
        }
    }

    /**
     * Payment succeeded: update order to PAID
     * This is the happy path where customer's card was charged successfully
     */
    private fun handleChargeSucceeded(dataObject: JsonObject): ResponseEntity<*> {
        return try {
            val metadata = dataObject.getAsJsonObject("metadata")
            val orderId = metadata.get("orderId")?.asString
                ?: throw Exception("No orderId in charge metadata")
            val customerId = metadata.get("customerId")?.asString
                ?: throw Exception("No customerId in charge metadata")

            val order = orderRepository.findByIdAndCustomerId(
                id = UUID.fromString(orderId),
                customerId = UUID.fromString(customerId)
            ) ?: throw Exception("Order not found: $orderId")

            if (order.paymentStatus == PaymentStatus.PAID) {
                logger.info("Order $orderId already marked as PAID, skipping")
                return ResponseEntity.ok("Already processed")
            }

            orderRepository.save(
                order.copy(
                    paymentStatus = PaymentStatus.PAID,
                    status = OrderStatus.PLACED
                )
            )

            logger.info("✅ Order $orderId payment confirmed via webhook. Status: PAID")
            ResponseEntity.ok("Payment confirmed")
        } catch (e: Exception) {
            logger.error("Error handling charge.succeeded: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Payment failed: log it but don't change order status
     * Order stays DRAFT so customer can retry with a different card
     */
    private fun handlePaymentFailed(dataObject: JsonObject): ResponseEntity<*> {
        return try {
            val metadata = dataObject.getAsJsonObject("metadata")
            val orderId = metadata.get("orderId")?.asString
                ?: throw Exception("No orderId in intent metadata")
            val lastError = dataObject.getAsJsonObject("last_payment_error")
            val errorMessage = lastError?.get("message")?.asString ?: "Unknown error"

            logger.warn("❌ Payment failed for order $orderId: $errorMessage")

            ResponseEntity.ok("Payment failure logged")
        } catch (e: Exception) {
            logger.error("Error handling payment_intent.payment_failed: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Refund processed: update order to REFUNDED
     * Customer requested refund or you issued one
     */
    private fun handleChargeRefunded(dataObject: JsonObject): ResponseEntity<*> {
        return try {
            val metadata = dataObject.getAsJsonObject("metadata")
            val orderId = metadata.get("orderId")?.asString
                ?: throw Exception("No orderId in charge metadata")
            val customerId = metadata.get("customerId")?.asString
                ?: throw Exception("No customerId in charge metadata")
            val amountRefunded = dataObject.get("amount_refunded")?.asLong ?: 0L

            val order = orderRepository.findByIdAndCustomerId(
                id = UUID.fromString(orderId),
                customerId = UUID.fromString(customerId)
            ) ?: throw Exception("Order not found: $orderId")

            orderRepository.save(
                order.copy(
                    paymentStatus = PaymentStatus.REFUNDED
                )
            )

            logger.info("💰 Order $orderId refunded. Amount: $amountRefunded cents")
            ResponseEntity.ok("Refund processed")
        } catch (e: Exception) {
            logger.error("Error handling charge.refunded: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Chargeback/dispute filed: flag order for review
     * Customer disputed the charge with their bank
     */
    private fun handleChargeDispute(dataObject: JsonObject): ResponseEntity<*> {
        return try {
            val metadata = dataObject.getAsJsonObject("metadata")
            val orderId = metadata.get("orderId")?.asString
                ?: throw Exception("No orderId in charge metadata")
            val customerId = metadata.get("customerId")?.asString
                ?: throw Exception("No customerId in charge metadata")
            val reason = dataObject.get("reason")?.asString ?: "Unknown"

            logger.error("⚠️ DISPUTE FILED for order $orderId - requires manual review!")
            logger.error("Dispute reason: $reason")

            try {
                emailService.sendStripeChargeDisputeEmail(
                    orderId = orderId,
                    customerId = customerId,
                    disputeReason = reason
                )
            } catch (e: Exception) {
                logger.warn("Failed to send dispute alert email: ${e.message}")
            }

            ResponseEntity.ok("Dispute noted")
        } catch (e: Exception) {
            logger.error("Error handling charge.dispute.created: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }

    /**
     * Fallback for payment_intent.succeeded (alternative event)
     * Some Stripe events send payment_intent instead of charge
     */
    private fun handlePaymentIntentSucceeded(dataObject: JsonObject): ResponseEntity<*> {
        return try {
            val metadata = dataObject.getAsJsonObject("metadata")
            val orderId = metadata.get("orderId")?.asString
                ?: throw Exception("No orderId in intent metadata")
            val customerId = metadata.get("customerId")?.asString
                ?: throw Exception("No customerId in intent metadata")

            val order = orderRepository.findByIdAndCustomerId(
                id = UUID.fromString(orderId),
                customerId = UUID.fromString(customerId)
            ) ?: throw Exception("Order not found: $orderId")

            if (order.paymentStatus == PaymentStatus.PAID) {
                return ResponseEntity.ok("Already processed")
            }

            orderRepository.save(
                order.copy(
                    paymentStatus = PaymentStatus.PAID,
                    status = OrderStatus.PLACED
                )
            )

            logger.info("✅ Order $orderId payment confirmed via payment_intent.succeeded")
            ResponseEntity.ok("Payment confirmed")
        } catch (e: Exception) {
            logger.error("Error handling payment_intent.succeeded: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event")
        }
    }
}