package com.example.sharpoclockserver.product

import com.example.sharpoclockserver.model.SemanticWatchSearchPage
import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * RAG based recommendation by customer profile info
 * in the mobile app this will be a header section with (Recommended For You) title and a horizontal list of watches (can be limited to 10)
 *
 * POST http://localhost:8080/api/watches/recommended(customer_profile)
 */
// GET http://localhost:8080/api/watches?page=1&size=20
// GET http://localhost:8080/api/watches/watch?id=1234
// GET http://localhost:8080/api/watches/search?q=that_blue_expensive_watch_from_rolex&size=25 : initiate semantic search
// GET http://localhost:8080/api/watches/search/{searchId}?q=that_blue_expensive_watch_from_rolex&page=2&size=25 : get cached search results by page
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

    @GetMapping("/watch")
    fun getWatchDetailsById(
        @RequestParam(value = "id") watchId: UUID,
    ): ResponseEntity<Optional<WatchForDisplay>> = watchService.getWatchDetailsById(watchId)

    @GetMapping("/search")
    fun initSemanticSearchAndCacheResults(
        @RequestParam(value = "q") query: String,
        @RequestParam(value = "size", defaultValue = "25") size: Int,
    ): ResponseEntity<SemanticWatchSearchPage> =
        watchService.initSemanticSearchPagedAndCacheResults(query = query, size = size)

    @GetMapping("/search/{searchId}")
    fun getSemanticSearchResultsByIdPaged(
        @PathVariable(value = "searchId") searchId: UUID,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "25") size: Int,
    ): ResponseEntity<SemanticWatchSearchPage> =
        watchService.getSemanticSearchResultsByIdPaged(searchId = searchId, page = page, size = size)
}