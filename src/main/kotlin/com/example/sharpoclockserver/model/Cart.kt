package com.example.sharpoclockserver.model

import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * <u>Mini Carts :</u>
 * <br><br>
 *
 * - We will be saving multiple carts per user.
 * <br>
 * - Each cart has a label defined by the customer (Retro watches, Classy watches, Blue collection, ...Etc).
 * <br>
 * - "Add to cart" in the client-side will prompt a cart selection dialog to the customer.
 *
 */

@Entity
@Table(
    name = "carts",
    indexes = [
        Index(
            name = "idx_carts_customer_id",
            columnList = "customer_id"
        )
    ]
)
data class Cart(
    @Id
    @Column(columnDefinition = "uuid", nullable = false, unique = true, updatable = false)
    val id: UUID,
    @ManyToOne(
        targetEntity = Customer::class,
        fetch = FetchType.LAZY,
        optional = false,
    )
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    val customer: Customer,

    @ElementCollection
    @CollectionTable(
        name = "cart_lines",
        joinColumns = [JoinColumn(name = "cart_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uc_cart_watch_size",
                columnNames = ["cart_id", "watch_id", "watch_size"],
            )
        ]
    )
    val cartLines: MutableList<CartLine> = mutableListOf(),

    @Column(nullable = true, length = 80)
    var label: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
)

@Embeddable
data class CartLine(
    @ManyToOne(
        targetEntity = Watch::class,
        fetch = FetchType.LAZY,
        optional = false,
    )
    @JoinColumn(name = "watch_id", nullable = false, updatable = false)
    val watch: Watch,

    @Column(name = "watch_size", nullable = false, updatable = true)
    var size: Int,
    @Column(nullable = false)
    var quantity: Int
)