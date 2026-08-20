package com.example.sharpoclockserver.model

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "customers")
data class Customer(
    @Id
    @Column(columnDefinition = "uuid", unique = true, nullable = false, updatable = false)
    val id: UUID,

    @Column(nullable = false, unique = true, updatable = false)
    val email: String,
    @Column(name = "full_name", nullable = false)
    val fullName: String,
    @Column(nullable = false, unique = true)
    val pseudo: String,
    @Column(nullable = false)
    var password: String,

    @Column(name = "phone_number", unique = true)
    val phoneNumber: String? = null,
    @Column(name = "address_line")
    val addressLine: String? = null,
    /** will be used for the recommendation system (AI recommends articles (watches) depending on customer age) */
    @Column
    val age: Int? = null,

    @ElementCollection
    @CollectionTable(
        name = "customer_registered_devices",
        joinColumns = [JoinColumn(name = "customer_id")],
    )
    var lastSignedInDeviceList: List<RegisteredDevice>,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)
