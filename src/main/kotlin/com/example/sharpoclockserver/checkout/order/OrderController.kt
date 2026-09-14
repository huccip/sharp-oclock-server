package com.example.sharpoclockserver.checkout.order

import com.example.sharpoclockserver.model.OrderStatus
import com.example.sharpoclockserver.model.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

// GET http://localhost:8080/checkout/orders: gets all orders that belong to the authenticated customer
// GET http://localhost:8080/checkout/orders/{orderId}: gets the order info by orderId

// POST http://localhost:8080/checkout/orders/drafts: creates a draft order based on a cart
// PATCH http://localhost:8080/checkout/orders/{orderId}/delivery: saves delivery information
// PATCH http://localhost:8080/checkout/orders/{orderId}/payment: saves selected payment method
// POST http://localhost:8080/checkout/orders/{orderId}/confirm: validates the checkout then places the order
// POST http://localhost:8080/checkout/orders/{orderId}/cancel: cancels a draft or placed order

@RestController
@RequestMapping("/checkout/orders")
class OrderController(
    private val orderService: OrderService
) {

    data class CreateDraftOrderRequest(
        val cartId: UUID,
    )

    data class DeliveryInfoRequest(
        @NotBlank
        @Size(max = 120)
        val fullName: String,
        @NotBlank
        @Size(max = 100)
        val city: String,
        @NotBlank
        @Size(max = 300)
        val address: String,
        @NotBlank
        @Size(max = 30)
        val phoneNumber: String,
    )

    data class PaymentMethodRequest(
        val paymentMethod: PaymentMethod,
    )

    data class OrderResponse(
        val id: UUID,
        val sourceCartId: UUID,
        val totalPriceCents: Long,
        val deliveryInfo: DeliveryInfoResponse?,
        val paymentMethod: PaymentMethod?,
        val orderLines: List<OrderLineResponse>,
        val status: OrderStatus,
        val createdAt: Instant,
        val updatedAt: Instant?,
    )

    data class DeliveryInfoResponse(
        val fullName: String,
        val city: String,
        val address: String,
        val phoneNumber: String,
    )

    data class OrderLineResponse(
        val watchId: UUID,
        val watchBrand: String,
        val watchName: String,
        val size: Int,
        val quantity: Int,
        val unitPriceAtPurchase: Int,
    )

    @GetMapping
    fun getAllOrders(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> = orderService.getAllOrders(jwt)

    @GetMapping("/{orderId}")
    fun getOrderById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderId: UUID,
    ): ResponseEntity<*> = orderService.getOrderById(jwt = jwt, orderId = orderId)

    @PostMapping("/drafts")
    fun createDraftOrder(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid request: CreateDraftOrderRequest,
    ): ResponseEntity<*> = orderService.createDraftOrder(jwt = jwt, request = request)

    @PatchMapping("/{orderId}/delivery")
    fun updateDeliveryInfo(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderId: UUID,
        @RequestBody @Valid request: DeliveryInfoRequest,
    ): ResponseEntity<*> = orderService.updateDeliveryInfo(
        jwt = jwt,
        orderId = orderId,
        request = request,
    )

    @PatchMapping("/{orderId}/payment")
    fun updatePaymentMethod(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderId: UUID,
        @RequestBody @Valid request: PaymentMethodRequest,
    ): ResponseEntity<*> = orderService.updatePaymentMethod(
        jwt = jwt,
        orderId = orderId,
        request = request,
    )

    @PostMapping("/{orderId}/confirm")
    fun confirmOrder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderId: UUID,
    ): ResponseEntity<*> = orderService.confirmOrder(jwt = jwt, orderId = orderId)

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderId: UUID,
    ): ResponseEntity<*> = orderService.cancelOrder(jwt = jwt, orderId = orderId)

}