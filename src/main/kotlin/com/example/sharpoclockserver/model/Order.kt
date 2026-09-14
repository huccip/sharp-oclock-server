package com.example.sharpoclockserver.model

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(
            name = "idx_orders_customer_id",
            columnList = "customer_id"
        )
    ]
)
data class Order(
    @Id
    @Column(columnDefinition = "uuid", nullable = false, unique = true, updatable = false)
    val id: UUID,

    @ManyToOne(
        fetch = FetchType.LAZY,
        targetEntity = Customer::class,
        optional = false,
    )
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    val customer: Customer,

    @Column(name = "source_cart_id", columnDefinition = "uuid", nullable = false, updatable = false)
    val sourceCartId: UUID,

    @Column(name = "total_price_cents", nullable = false)
    var totalPriceCents: Long,
    @Embedded
    var deliveryInfo: DeliveryInfo? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    var paymentMethod: PaymentMethod? = null,

    @ElementCollection
    @CollectionTable(
        name = "order_lines",
        joinColumns = [JoinColumn(name = "order_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uc_order_line",
                columnNames = ["order_id", "watch_id", "watch_size"]
            )
        ]
    )
    var orderLines: MutableList<OrderLine> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.DRAFT,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    var paymentStatus: PaymentStatus? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    @Column(name = "cash_on_delivery_payment_id")
    var cashOnDeliveryPaymentId: String? = null,
    @Column(name = "stripe_payment_intent_id")
    var stripePaymentIntentId: String? = null,
    @Column(name = "paypal_payment_intent_id")
    var paypalPaymentIntentId: String? = null,
)

@Embeddable
data class DeliveryInfo(
    @Column(name = "full_name")
    val fullName: String,
    @Column
    var city: String,
    @Column
    var address: String,
    @Column(name = "phone_number")
    var phoneNumber: String,
)

@Embeddable
data class OrderLine(
    @Id
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "watch_id", columnDefinition = "uuid", nullable = false, updatable = false)
    val watchId: UUID,

    @Column(name = "watch_brand", nullable = false)
    var watchBrand: String,
    @Column(name = "watch_name", nullable = false)
    var watchName: String,

    @Column(name = "watch_size", nullable = false)
    val size: Int,
    @Column(nullable = false)
    val quantity: Int,
    @Column(name = "unit_price_at_purchase", nullable = false)
    var unitPriceAtPurchase: Int,

    @Column(name = "eligible_for_refund", nullable = false)
    var eligibleForRefund: Boolean = true, // switches to false if refund duration in days ends (orderPlacedAt + CONSTANT_IN_DAYS)
    var refunded: Boolean = false,
)

enum class OrderStatus {
    DRAFT,
    PLACED,
    DELIVERED,
    CANCELLED
}

enum class PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
    CANCELLED
}

enum class PaymentMethod {
    CASH_ON_DELIVERY,
    STRIPE,
    PAYPAL
}