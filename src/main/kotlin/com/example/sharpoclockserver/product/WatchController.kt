package com.example.sharpoclockserver.product

import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * RAG based recommendation by age (if provided)
 * in the mobile app this will be a header section with (Recommended For You) title and a horizontal list of watches (can be limited to 10)
 *
 * POST http://localhost:8080/api/watches/recommended(age)
 */
// GET http://localhost:8080/api/watches?page=1&size=20
// GET http://localhost:8080/api/watches?id=1234
// GET http://localhost:8080/api/watches/search?q=that_blue_expensive_watch_from_rolex : RAG based search
// POST http://localhost:8080/api/watches/review(customerId, productId, review) : only for customers who bought a product

@RestController
@RequestMapping("/api/watches")
class WatchController(
    private val watchService: WatchService
) {

    data class WatchForDisplay(
        val id: UUID,

        val thumbnailUrl: String,
        val thumbnailPublicId: String,
        val heroUrl: String,
        val heroPublicId: String,

        val brand: String,
        val name: String,
        val description: String,
        val sizes: List<String>,
        val price: Int,
        val unitsSold: Long,

        val isOOS: Boolean,
        val isDiscounted: Boolean,
        val isNew: Boolean,
        val isPopular: Boolean,

        val discountPercentage: Int? = null,
    )

    @GetMapping
    fun getAllWatchesPaged(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "25") size: Int,
    ): ResponseEntity<PagedModel<WatchForDisplay>> = watchService.getAllWatchesPaged(page = page, size = size)
}