package com.example.sharpoclockserver.checkout.order

import com.example.sharpoclockserver.checkout.order.payment.cash.CashOnDeliveryService
import com.example.sharpoclockserver.checkout.order.payment.stripe.StripeService
import com.example.sharpoclockserver.model.*
import com.example.sharpoclockserver.model.repository.CartRepository
import com.example.sharpoclockserver.model.repository.OrderRepository
import com.example.sharpoclockserver.model.repository.WatchRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val watchRepository: WatchRepository,

    private val cashOnDeliveryService: CashOnDeliveryService,
    private val stripeService: StripeService
) {

    @Transactional(readOnly = true)
    fun getAllOrders(jwt: Jwt): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val orders = orderRepository
            .findAllByCustomerIdOrderByCreatedAtDesc(customerId)
            .map { order -> order.toOrderResponse() }

        return ResponseEntity(orders, HttpStatus.OK)
    }

    @Transactional(readOnly = true)
    fun getOrderById(
        jwt: Jwt,
        orderId: UUID,
    ): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val order = orderRepository.findByIdAndCustomerId(orderId, customerId) ?: return notFound()

        return ResponseEntity(order.toOrderResponse(), HttpStatus.OK)
    }

    @Transactional
    fun createDraftOrder(
        jwt: Jwt,
        request: OrderController.CreateDraftOrderRequest,
    ): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val cart = cartRepository.findOwnedByIdForUpdate(request.cartId, customerId)
            ?: return notFound("cart")

        if (cart.cartLines.isEmpty()) return badRequest("The cart is empty")

        val orderLines = createOrderLines(cart) ?: return badRequest(
            "The cart contains an unavailable watch or watch size"
        )
        val totalPriceCents = calculateTotal(orderLines)
        val existingDraft = orderRepository.findByCustomerIdAndSourceCartIdAndStatus(
            customerId = customerId,
            sourceCartId = cart.id,
            status = OrderStatus.DRAFT,
        )

        if (existingDraft != null) {
            existingDraft.orderLines.clear()
            existingDraft.orderLines.addAll(orderLines)
            existingDraft.totalPriceCents = totalPriceCents
            existingDraft.updatedAt = Instant.now()

            return ResponseEntity(
                orderRepository.save(existingDraft).toOrderResponse(),
                HttpStatus.OK,
            )
        }

        val order = orderRepository.save(
            Order(
                id = UUID.randomUUID(),
                customer = cart.customer,
                sourceCartId = cart.id,
                totalPriceCents = totalPriceCents,
                orderLines = orderLines,
            )
        )

        return ResponseEntity(order.toOrderResponse(), HttpStatus.CREATED)
    }

    @Transactional
    fun updateDeliveryInfo(
        jwt: Jwt,
        orderId: UUID,
        request: OrderController.DeliveryInfoRequest,
    ): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val order = orderRepository.findOwnedByIdForUpdate(orderId, customerId) ?: return notFound()

        if (order.status != OrderStatus.DRAFT) return conflict("Only a draft order can be updated")

        order.deliveryInfo = DeliveryInfo(
            fullName = request.fullName.trim(),
            city = request.city.trim(),
            address = request.address.trim(),
            phoneNumber = request.phoneNumber.trim(),
        )
        order.updatedAt = Instant.now()

        return ResponseEntity(
            orderRepository.save(order).toOrderResponse(),
            HttpStatus.OK,
        )
    }

    @Transactional
    fun updatePaymentMethod(
        jwt: Jwt,
        orderId: UUID,
        request: OrderController.PaymentMethodRequest,
    ): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val order = orderRepository.findOwnedByIdForUpdate(orderId, customerId) ?: return notFound()

        if (order.status != OrderStatus.DRAFT) return conflict("Only a draft order can be updated")

        order.paymentMethod = request.paymentMethod
        order.updatedAt = Instant.now()

        return ResponseEntity(
            orderRepository.save(order).toOrderResponse(),
            HttpStatus.OK,
        )
    }

    @Transactional
    fun confirmOrder(
        jwt: Jwt,
        orderId: UUID,
    ): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val order = orderRepository.findOwnedByIdForUpdate(orderId, customerId) ?: return notFound()

        if (order.status != OrderStatus.DRAFT) return conflict("Only a draft order can be confirmed")
        if (order.deliveryInfo == null) return badRequest("Delivery information is missing")

        val paymentMethod = order.paymentMethod ?: return badRequest("Payment method is missing")

        when (paymentMethod) {
            PaymentMethod.CASH_ON_DELIVERY -> {
                val paymentResult = cashOnDeliveryService.createPayment(
                    orderId = orderId.toString(),
                    customerId = customerId.toString()
                )
                if (paymentResult.statusCode != HttpStatus.CREATED && paymentResult.statusCode != HttpStatus.OK) {
                    return paymentResult
                }
            }

            PaymentMethod.STRIPE -> {
                val paymentResult = stripeService.createPaymentIntent(
                    orderId = orderId.toString(),
                    customerId = customerId.toString()
                )
                if (paymentResult.statusCode != HttpStatus.CREATED && paymentResult.statusCode != HttpStatus.OK) {
                    return paymentResult
                }
            }

            PaymentMethod.PAYPAL -> {
                return ResponseEntity(
                    "Not implemented yet",
                    HttpStatus.NOT_IMPLEMENTED
                )
            }
        }

        val watchesById = watchRepository
            .findAllByIdForUpdate(order.orderLines.map { line -> line.watchId })
            .associateBy(Watch::id)

        val currentWatches = mutableMapOf<UUID, Watch>()
        for (line in order.orderLines) {
            val watch = watchesById[line.watchId]
                ?: return conflict("${line.watchBrand} ${line.watchName} is no longer available")

            if (watch.isOOS) return conflict("${watch.brand} ${watch.name} is out of stock")
            if (line.size !in watch.sizes) return conflict(
                "Size ${line.size}mm is no longer available for ${watch.brand} ${watch.name}"
            )

            currentWatches[line.watchId] = watch
        }

        for (line in order.orderLines) {
            val watch = currentWatches.getValue(line.watchId)
            line.watchBrand = watch.brand
            line.watchName = watch.name
            line.unitPriceAtPurchase = watch.currentPriceCents()
        }

        order.totalPriceCents = calculateTotal(order.orderLines)
        order.status = OrderStatus.PLACED
        order.updatedAt = Instant.now()

        watchesById.values.forEach { watch ->
            val quantity = order.orderLines
                .filter { line -> line.watchId == watch.id }
                .sumOf { line -> line.quantity }
            watch.unitsSold += quantity
        }

        cartRepository.findById(order.sourceCartId).orElse(null)
            ?.takeIf { cart -> cart.customer.id == customerId }
            ?.let { cart ->
                cart.cartLines.clear()
                cart.updatedAt = Instant.now()
                cartRepository.save(cart)
            } ?: return notFound("cart")

        return ResponseEntity(
            orderRepository.save(order).toOrderResponse(),
            HttpStatus.OK,
        )
    }

    @Transactional
    fun cancelOrder(
        jwt: Jwt,
        orderId: UUID,
    ): ResponseEntity<*> {
        val customerId = customerId(jwt) ?: return unauthorized()
        val order = orderRepository.findOwnedByIdForUpdate(orderId, customerId) ?: return notFound()

        if (order.status == OrderStatus.DELIVERED) return conflict("A delivered order cannot be cancelled")
        if (order.status == OrderStatus.CANCELLED) return conflict("The order is already cancelled")

        if (order.status == OrderStatus.PLACED) {
            val watchesById = watchRepository
                .findAllByIdForUpdate(order.orderLines.map { line -> line.watchId })
                .associateBy(Watch::id)

            watchesById.values.forEach { watch ->
                val quantity = order.orderLines
                    .filter { line -> line.watchId == watch.id }
                    .sumOf { line -> line.quantity }
                watch.unitsSold = (watch.unitsSold - quantity).coerceAtLeast(0)
            }
        }

        order.status = OrderStatus.CANCELLED
        order.updatedAt = Instant.now()

        return ResponseEntity(
            orderRepository.save(order).toOrderResponse(),
            HttpStatus.OK,
        )
    }

    private fun createOrderLines(cart: Cart): MutableList<OrderLine>? {
        val orderLines = mutableListOf<OrderLine>()

        for (cartLine in cart.cartLines) {
            val watch = cartLine.watch
            if (watch.isOOS || cartLine.size !in watch.sizes) return null

            orderLines.add(
                OrderLine(
                    id = UUID.randomUUID(),
                    watchId = watch.id,
                    watchBrand = watch.brand,
                    watchName = watch.name,
                    size = cartLine.size,
                    quantity = cartLine.quantity,
                    unitPriceAtPurchase = watch.currentPriceCents(),
                )
            )
        }

        return orderLines
    }

    private fun calculateTotal(orderLines: List<OrderLine>): Long =
        orderLines.sumOf { line -> line.unitPriceAtPurchase.toLong() * line.quantity }

    private fun Watch.currentPriceCents(): Int {
        val percentage = discountPercentage
            ?.takeIf { isDiscounted }
            ?: return price

        return ((price.toLong() * (100 - percentage) + 50) / 100).toInt()
    }

    private fun Order.toOrderResponse() = OrderController.OrderResponse(
        id = id,
        sourceCartId = sourceCartId,
        totalPriceCents = totalPriceCents,
        deliveryInfo = deliveryInfo?.let { info ->
            OrderController.DeliveryInfoResponse(
                fullName = info.fullName,
                city = info.city,
                address = info.address,
                phoneNumber = info.phoneNumber,
            )
        },
        paymentMethod = paymentMethod,
        orderLines = orderLines.map { line ->
            OrderController.OrderLineResponse(
                watchId = line.watchId,
                watchBrand = line.watchBrand,
                watchName = line.watchName,
                size = line.size,
                quantity = line.quantity,
                unitPriceAtPurchase = line.unitPriceAtPurchase,
            )
        },
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun customerId(jwt: Jwt): UUID? =
        runCatching { UUID.fromString(jwt.subject) }.getOrNull()

    private fun badRequest(message: String): ResponseEntity<*> =
        ResponseEntity(message, HttpStatus.BAD_REQUEST)

    private fun conflict(message: String): ResponseEntity<*> =
        ResponseEntity(message, HttpStatus.CONFLICT)

    private fun notFound(resource: String = "order"): ResponseEntity<*> =
        ResponseEntity("The requested $resource was not found", HttpStatus.NOT_FOUND)

    private fun unauthorized(): ResponseEntity<*> =
        ResponseEntity("You are unauthorized to make this request", HttpStatus.UNAUTHORIZED)
}