package com.example.sharpoclockserver.checkout.order.payment.paypal

import com.example.sharpoclockserver.email.EmailService
import com.example.sharpoclockserver.model.OrderStatus
import com.example.sharpoclockserver.model.PaymentStatus
import com.example.sharpoclockserver.model.repository.OrderRepository
import com.paypal.sdk.PaypalServerSdkClient
import com.paypal.sdk.models.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PaypalService(
    private val orderRepository: OrderRepository,
    private val paypalClient: PaypalServerSdkClient,
    private val emailService: EmailService,
    @Value($$"${app.email.from}")
    private val paymentReceiptEmail: String,
) {

    @Transactional
    fun createPaymentIntent(
        orderId: String,
        customerId: String? = null,
    ): ResponseEntity<*> {

        if (customerId == null) return unauthorized()

        val order = orderRepository.findByIdAndCustomerId(
            id = UUID.fromString(orderId),
            customerId = UUID.fromString(customerId),
        ) ?: return notFound()

        if (order.status != OrderStatus.DRAFT) return badRequest("order status is NOT DRAFT")
        if (order.paymentStatus != PaymentStatus.PENDING) return badRequest("payment status is NOT PENDING")

        val orderRequest = OrderRequest.Builder()
            .intent(CheckoutPaymentIntent.CAPTURE)
            .payer(
                Payer.Builder()
                    .payerId(customerId)
                    .build()
            )
            .purchaseUnits(
                listOf(
                    PurchaseUnitRequest.Builder()
                        .referenceId(orderId)
                        .description("Customer $customerId wants to pay for the order $orderId")
                        .amount(
                            AmountWithBreakdown.Builder()
                                .currencyCode("MAD")
                                .value(order.totalPriceCents.div(100).toString())
                                .build()
                        )
                        .build()
                )
            )
            .build()

        val paypalOrderApiResponse = paypalClient.ordersController.createOrder(
            CreateOrderInput.Builder()
                .body(orderRequest)
                .build()
        )

        val paypalOrderId = paypalOrderApiResponse.result.id
        val approvalUrl = paypalOrderApiResponse.result.links
            .find { it.rel == "approve" }
            ?.href ?: return conflict("the approval link was not found")

        orderRepository.save(
            order.copy(
                paymentStatus = PaymentStatus.PENDING,
                paypalPaymentIntentId = paypalOrderId
            )
        )

        return ResponseEntity.ok(
            PaypalController.PaymentIntentResponse(
                orderId = orderId,
                paypalOrderId = paypalOrderId,
                approvalUrl = approvalUrl
            )
        )
    }

    @Transactional
    fun confirmPayment(
        orderId: String,
        customerId: String? = null,
        confirmRequest: PaypalController.ConfirmRequest,
    ): ResponseEntity<*> {
        if (customerId == null) return unauthorized()

        val order = orderRepository.findByIdAndCustomerId(
            id = UUID.fromString(orderId),
            customerId = UUID.fromString(customerId),
        ) ?: return notFound()

        if (order.status != OrderStatus.DRAFT) return badRequest("order status is NOT DRAFT")

        val paypalOrderId = confirmRequest.paypalOrderId
        if (paypalOrderId != order.paypalPaymentIntentId) return badRequest("payment order id mismatch")

        val paypalOrder = paypalClient.ordersController.getOrder(
            GetOrderInput.Builder()
                .id(paypalOrderId)
                .build()
        )
        if (paypalOrder.result.payer.payerId != customerId) return unauthorized()

        val confirmResult = paypalClient.ordersController.confirmOrder(
            ConfirmOrderInput.Builder()
                .id(paypalOrderId)
                .build()
        )
        if (confirmResult.result.status != com.paypal.sdk.models.OrderStatus.COMPLETED) return conflict("PayPal order status is NOT COMPLETED")

        val updatedOrder = orderRepository.save(
            order.copy(
                status = OrderStatus.PLACED,
                paymentStatus = PaymentStatus.PAID,
            )
        )

        emailService.sendPaypalPaymentReceiptEmail(
            recipient = order.customer.email,
            customer = order.customer,
            orderId = orderId,
            totalPriceCents = order.totalPriceCents
        )

        // send a copy of the receipt to the dev team
        emailService.sendPaypalPaymentReceiptEmail(
            recipient = paymentReceiptEmail,
            customer = order.customer,
            orderId = orderId,
            totalPriceCents = order.totalPriceCents
        )

        return ResponseEntity.ok(
            PaypalController.ConfirmResponse(
                orderId = orderId,
                orderStatus = updatedOrder.status.name,
                paymentStatus = updatedOrder.paymentStatus!!.name,
                paypalPaymentStatus = confirmResult.result.status.name
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