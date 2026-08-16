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
    @Column(name = "phone_number", unique = true)
    val phoneNumber: String? = null,
    @Column(nullable = false)
    val password: String,
    @Column(name = "address_line")
    val addressLine: String? = null,
    @Column
    val age: Int? = null,
    /** will be used for the recommendation system (AI recommends articles (watches) depending on customer age) */

    @Column(name = "last_signed_in_at_list", nullable = false)
    val lastSignedInAtList: List<Instant>,

    @ElementCollection
    @CollectionTable(
        name = "customer_registered_devices",
        joinColumns = [JoinColumn(name = "customer_id")],
    )
    val lastSignedInDeviceList: List<RegisteredDevice>,
)
