package com.example.sharpoclockserver.checkout.order.payment.stripe

import com.example.sharpoclockserver.model.OrderStatus
import com.example.sharpoclockserver.model.PaymentStatus
import com.example.sharpoclockserver.model.repository.OrderRepository
import com.stripe.StripeClient
import com.stripe.param.PaymentIntentCreateParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class StripeService(
    private val orderRepository: OrderRepository,
    private val stripeClient: StripeClient,
    @Value($$"${app.mail.from}")
    private val paymentReceiptEmail: String,
) {

    fun createPaymentIntent(
        orderId: String,
        customerId: String? = null,
    ): ResponseEntity<*> {

        if (customerId == null) return unauthorized()

        val order = orderRepository.findByIdAndCustomerId(
            id = UUID.fromString(orderId),
            customerId = UUID.fromString(customerId)
        ) ?: return notFound()

        if (order.status != OrderStatus.DRAFT) return badRequest("order status is NOT DRAFT")
        if (order.paymentStatus != PaymentStatus.PENDING) return badRequest("payment status is NOT PENDING")

        val paymentIntentParams = PaymentIntentCreateParams.builder()
            .setReceiptEmail(paymentReceiptEmail)
            .setAmount(order.totalPriceCents)
            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
            .setCustomer(customerId)
            .setDescription("Customer $customerId wants to pay for the order $orderId")
            .setCurrency("MAD") // this is a local e-commerce platform
            .putAllMetadata(
                mapOf(
                    "orderId" to orderId,
                    "customerId" to customerId
                )
            )
            .build()

        val paymentIntent = stripeClient.v1().paymentIntents().create(paymentIntentParams)

        orderRepository.save(
            order.copy(
                paymentStatus = PaymentStatus.PENDING,
                stripePaymentIntentId = paymentIntent.id
            )
        )

        return ResponseEntity.ok(
            StripeController.PaymentIntentResponse(
                orderId = orderId,
                paymentIntentId = paymentIntent.id,
                clientSecret = paymentIntent.clientSecret
            )
        )
    }

    fun confirmPayment(
        orderId: String,
        customerId: String? = null,
        confirmRequest: StripeController.ConfirmRequest,
    ): ResponseEntity<*> {

        if (customerId == null) return unauthorized()

        val order = orderRepository.findByIdAndCustomerId(
            id = UUID.fromString(orderId),
            customerId = UUID.fromString(customerId),
        ) ?: return notFound()

        val paymentIntentId = confirmRequest.paymentIntentId
        if (paymentIntentId != order.stripePaymentIntentId) return badRequest("payment intent id mismatch")

        val paymentIntent = stripeClient.v1().paymentIntents().retrieve(paymentIntentId)
        val paymentMetadata = paymentIntent.metadata

        if (
            paymentMetadata["orderId"] != order.id.toString() ||
            paymentMetadata["customerId"] != customerId
        ) return unauthorized()

        if (paymentIntent.status != "succeeded") return conflict(
            "the payment was not succeeded",
        )

        val updatedOrder = orderRepository.save(
            order.copy(
                paymentStatus = PaymentStatus.PAID,
                status = OrderStatus.PLACED
            )
        )

        return ResponseEntity.ok(
            StripeController.ConfirmResponse(
                orderId = updatedOrder.id.toString(),
                orderStatus = updatedOrder.status.name,
                intentStatus = paymentIntent.status,
                paymentStatus = updatedOrder.paymentStatus!!.name
            )
        )
    }

    private fun notFound(resource: String = "order") = ResponseEntity(
        "Could not find $resource",
        HttpStatus.NOT_FOUND
    )

    private fun conflict(reason: String) = ResponseEntity(
        "There was a conflict because $reason",
        HttpStatus.CONFLICT
    )

    private fun badRequest(parameter: String) = ResponseEntity(
        "Could not process the request with a bad $parameter",
        HttpStatus.BAD_REQUEST
    )

    private fun unauthorized() = ResponseEntity(
        "You are unauthorized",
        HttpStatus.UNAUTHORIZED
    )
}