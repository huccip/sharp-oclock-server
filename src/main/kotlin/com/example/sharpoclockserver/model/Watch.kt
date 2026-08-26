package com.example.sharpoclockserver.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "watches")
data class Watch(
    @Id
    @Column(columnDefinition = "uuid", nullable = false, updatable = false, unique = true)
    val id: UUID,
    @Column(name = "thumbnail_url", length = 1024, nullable = false)
    var thumbnailUrl: String,
    @Column(name = "thumbnail_public_id", nullable = false)
    var thumbnailPublicId: String,
    @Column(name = "hero_url", length = 1024, nullable = false)
    var heroUrl: String,
    @Column(name = "hero_public_id", nullable = false)
    var heroPublicId: String,
    @Column(nullable = false)
    var brand: String,
    @Column(nullable = false)
    var name: String,
    @Column(columnDefinition = "text", nullable = false)
    var description: String,
    @Column(nullable = false)
    var sizes: List<String>,
    @Column(nullable = false)
    var price: Int, // multiply by 100 to store and divide by 100 to retrieve
    @Column(name = "units_sold", nullable = false)
    var unitsSold: Long,

    @Column(name = "is_oos", nullable = false)
    var isOOS: Boolean = false, // Out Of Stock
    @Column(name = "is_discounted", nullable = false)
    var isDiscounted: Boolean = false,
    @Column(name = "is_new", nullable = false)
    var isNew: Boolean = true,
    @Column(name = "is_popular", nullable = false)
    var isPopular: Boolean = false, // true if units sold > 20 and the median of reviews >= 4.0

    @Column(name = "discount_percentage")
    var discountPercentage: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "last_edited_at", updatable = true)
    val lastEditedAt: Instant? = null
) {
    override fun toString(): String = """
        Brand: $brand
        Model: $name
        Description: $description
        Sizes: ${sizes.joinToString()}
        Price in USD: ${price / 100.0}
        Popularity: $isPopular
        New Arrival: $isNew
        Discounted: $isDiscounted
    """.trimIndent()
}
