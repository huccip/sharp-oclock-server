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
    var thumbnailUrl: String, // path to the small image (is stored in Object Storage Cloudflare R2 and is displayed in plp)
    @Column(name = "image_url", length = 1024, nullable = false)
    var imageUrl: String, // path to the big image (is stored in Object Storage Cloudflare R2 and is displayed in pdp)
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

    @Column(name = "is_oos")
    var isOOS: Boolean? = null, // Out Of Stock
    @Column(name = "is_discounted")
    var isDiscounted: Boolean? = null,
    @Column(name = "is_new")
    var isNew: Boolean? = null,
    @Column(name = "is_popular")
    var isPopular: Boolean? = null, // true if units sold > 20 and the median of reviews >= 4.0

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "last_edited_at", updatable = true)
    val lastEditedAt: Instant? = null
)
