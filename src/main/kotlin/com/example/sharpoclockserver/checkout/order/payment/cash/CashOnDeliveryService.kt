package com.example.sharpoclockserver.checkout.order.payment.cash

import com.example.sharpoclockserver.model.PaymentMethod
import com.example.sharpoclockserver.model.PaymentStatus
import com.example.sharpoclockserver.model.repository.OrderRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class CashOnDeliveryService(
    private val orderRepository: OrderRepository
) {

    @Transactional
    fun createPayment(
        orderId: String,
        customerId: String?,
    ): ResponseEntity<*> {
        if (customerId == null) return notFound("customer")

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound()

        if (order.paymentStatus != null) {
            return ResponseEntity(
                CashOnDeliveryController.PaymentResponse(
                    orderId = order.id.toString(),
                    paymentId = order.cashOnDeliveryPaymentId ?: "",
                    paymentMethod = PaymentMethod.CASH_ON_DELIVERY.name,
                    paymentStatus = order.paymentStatus!!.name,
                    amountInCents = order.totalPriceCents
                ),
                HttpStatus.OK
            )
        }

        val paymentId = UUID.randomUUID().toString()

        val updatedOrder = orderRepository.save(
            order.copy(
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                cashOnDeliveryPaymentId = paymentId,
                paymentStatus = PaymentStatus.PENDING,
                updatedAt = Instant.now()
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.PaymentResponse(
                orderId = updatedOrder.id.toString(),
                paymentId = paymentId,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY.name,
                paymentStatus = PaymentStatus.PENDING.name,
                amountInCents = updatedOrder.totalPriceCents
            ),
            HttpStatus.CREATED
        )
    }

    @Transactional
    fun confirmPayment(
        orderId: String,
        customerId: String,
    ): ResponseEntity<*> {
        val admin = SecurityContextHolder.getContext().authentication
        if (!isAdminAuthenticated(admin)) {
            return unauthorized()
        }

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound()

        if (order.paymentStatus == null || order.paymentStatus != PaymentStatus.PENDING) {
            return ResponseEntity(
                "Payment must be in PENDING status to confirm",
                HttpStatus.CONFLICT
            )
        }

        val updatedOrder = orderRepository.save(
            order.copy(
                paymentStatus = PaymentStatus.PAID,
                updatedAt = Instant.now()
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.PaymentResponse(
                orderId = updatedOrder.id.toString(),
                paymentId = updatedOrder.cashOnDeliveryPaymentId ?: "",
                paymentMethod = updatedOrder.paymentMethod!!.name,
                paymentStatus = PaymentStatus.PAID.name,
                amountInCents = updatedOrder.totalPriceCents
            ),
            HttpStatus.OK
        )
    }

    @Transactional
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

        val orderLineToRefund = order.orderLines.find {
            it.id == UUID.fromString(refundRequest.orderLineId)
        } ?: return notFound("order line")

        if (!orderLineToRefund.eligibleForRefund) {
            return ResponseEntity(
                "The refund duration has expired for this watch",
                HttpStatus.NOT_ACCEPTABLE
            )
        }

        if (orderLineToRefund.refunded) {
            return ResponseEntity(
                "This order line has already been refunded",
                HttpStatus.NOT_MODIFIED
            )
        }

        orderLineToRefund.refunded = true
        orderLineToRefund.eligibleForRefund = false

        val totalPriceInCentsToRefund = orderLineToRefund.unitPriceAtPurchase * orderLineToRefund.quantity.toLong()

        val updatedPaymentStatus = if (order.orderLines.all { it.refunded }) {
            PaymentStatus.REFUNDED
        } else {
            order.paymentStatus
        }

        val updatedOrder = orderRepository.save(
            order.copy(
                totalPriceCents = (order.totalPriceCents - totalPriceInCentsToRefund).coerceAtLeast(0),
                paymentStatus = updatedPaymentStatus,
                updatedAt = Instant.now()
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.RefundResponse(
                orderId = orderId,
                orderLineId = refundRequest.orderLineId,
                paymentMethod = updatedOrder.paymentMethod!!.name,
                paymentStatus = updatedOrder.paymentStatus!!.name,
                refundAmountInCents = totalPriceInCentsToRefund,
                orderTotalAmountInCents = updatedOrder.totalPriceCents
            ),
            HttpStatus.OK
        )
    }

    @Transactional
    fun cancel(
        orderId: String,
        customerId: String?,
    ): ResponseEntity<*> {
        if (customerId == null) return notFound("customer")

        val order = orderRepository.findByIdAndCustomerId(
            customerId = UUID.fromString(customerId),
            id = UUID.fromString(orderId)
        ) ?: return notFound("order")

        if (order.paymentStatus == PaymentStatus.CANCELLED) {
            return ResponseEntity(
                "Payment is already cancelled",
                HttpStatus.NOT_MODIFIED
            )
        }

        val updatedOrder = orderRepository.save(
            order.copy(
                paymentStatus = PaymentStatus.CANCELLED,
                updatedAt = Instant.now()
            )
        )

        return ResponseEntity(
            CashOnDeliveryController.PaymentResponse(
                orderId = updatedOrder.id.toString(),
                paymentId = updatedOrder.cashOnDeliveryPaymentId ?: "",
                paymentMethod = updatedOrder.paymentMethod!!.name,
                paymentStatus = PaymentStatus.CANCELLED.name,
                amountInCents = updatedOrder.totalPriceCents
            ),
            HttpStatus.OK
        )
    }

    private fun isAdminAuthenticated(auth: Authentication?): Boolean =
        auth != null && auth.isAuthenticated && auth.authorities.any { it.authority == "ROLE_ADMIN" }

    private fun notFound(resource: String = "order") = ResponseEntity(
        "Could not find your requested $resource",
        HttpStatus.NOT_FOUND
    )

    private fun unauthorized() = ResponseEntity(
        "You are not authorized to perform this action",
        HttpStatus.UNAUTHORIZED
    )
}