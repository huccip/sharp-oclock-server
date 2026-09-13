package com.example.sharpoclockserver.checkout.order.payment.cash

import com.example.sharpoclockserver.model.PaymentMethod
import com.example.sharpoclockserver.model.repository.OrderRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.*

@Service
class CashOnDeliveryService(
    private val orderRepository: OrderRepository,
    private val adminUserDetailsService: UserDetailsService
) {

    fun createPayment(
        orderId: String,
        customerId: String?,
    ): ResponseEntity<*> {

        if (customerId == null) return notFound("customer")

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound()

        val paymentId = UUID.randomUUID().toString()
        val paymentStatus = "PENDING"

        orderRepository.save(
            order.copy(
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                cashOnDeliveryPaymentId = paymentId, // for upcoming updates (admins will handle it based on delivery agency feedback)
                paymentStatus = paymentStatus // awaiting confirmation from the delivery agency
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.PaymentResponse(
                orderId = order.id.toString(),
                paymentId = paymentId,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY.name,
                paymentStatus = paymentStatus,
                amountInCents = order.totalPriceCents
            ),
            HttpStatus.CREATED
        )
    }

    fun confirmPayment(
        orderId: String,
        confirmRequest: CashOnDeliveryController.ConfirmRequest,
    ): ResponseEntity<*> {

        val adminIdentifier = confirmRequest.adminIdentifier
        val adminPassword = confirmRequest.adminPassword

        try {
            val admin = adminUserDetailsService.loadUserByUsername(adminIdentifier)
            if (!admin.password.equals(adminPassword)) return unauthorized()
        } catch (e: UsernameNotFoundException) {
            return notFound(e.localizedMessage)
        }

        val customerId = confirmRequest.customerId

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound()

        val paymentStatus = "PAID"

        orderRepository.save(
            order.copy(
                paymentStatus = paymentStatus
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.PaymentResponse(
                orderId = order.id.toString(),
                paymentId = order.cashOnDeliveryPaymentId.toString(),
                paymentMethod = order.paymentMethod!!.name,
                paymentStatus = paymentStatus,
                amountInCents = order.totalPriceCents
            ),
            HttpStatus.OK
        )
    }

    fun refund(
        orderId: String,
        customerId: String?,
        refundRequest: CashOnDeliveryController.RefundRequest,
    ): ResponseEntity<*> {

        if (customerId == null) return notFound("customer")

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound()

        val orderLineId = refundRequest.orderLineId
        val orderLineToRefund =
            order.orderLines.find { it.id == UUID.fromString(orderLineId) } ?: return notFound("watch")

        if (!orderLineToRefund.eligibleForRefund) return ResponseEntity(
            "The refund duration has expired for this watch",
            HttpStatus.NOT_ACCEPTABLE
        )

        if (orderLineToRefund.refunded) return ResponseEntity(
            "This refund request has already been approved",
            HttpStatus.NOT_MODIFIED
        )

        orderLineToRefund.refunded = true
        orderLineToRefund.eligibleForRefund = false

        val totalPriceInCentsToRefund = orderLineToRefund.unitPriceAtPurchase * orderLineToRefund.quantity.toLong()

        order.orderLines.replaceAll { orderLine ->
            if (orderLine.id == UUID.fromString(orderLineId)) return@replaceAll orderLineToRefund

            orderLine
        }

        if (order.orderLines.all { it.refunded }) order.paymentStatus = "REFUNDED"

        orderRepository.save(
            order.copy(
                totalPriceCents = order.totalPriceCents - totalPriceInCentsToRefund
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.RefundResponse(
                orderId = orderId,
                orderLineId = orderLineId,
                paymentMethod = order.paymentMethod!!.name,
                paymentStatus = order.paymentStatus!!,
                refundAmountInCents = totalPriceInCentsToRefund,
                orderTotalAmountInCents = order.totalPriceCents
            ),
            HttpStatus.OK
        )
    }

    fun cancel(
        orderId: String,
        customerId: String?,
    ): ResponseEntity<*> {

        if (customerId == null) return notFound("customer")

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound("order")

        val paymentStatus = "CANCELLED"

        orderRepository.save(
            order.copy(
                paymentStatus = paymentStatus
            )
        )

        return ResponseEntity(
            "Order $orderId is cancelled!",
            HttpStatus.OK
        )
    }

    private fun notFound(resource: String = "order") = ResponseEntity(
        "Could not find your requested $resource",
        HttpStatus.BAD_REQUEST
    )

    private fun unauthorized() = ResponseEntity(
        "You are not authorized to perform this action",
        HttpStatus.UNAUTHORIZED
    )
}