package com.example.sharpoclockserver.product

import com.example.sharpoclockserver.model.PageMetadata
import com.example.sharpoclockserver.model.SemanticWatchSearchPage
import com.example.sharpoclockserver.model.Watch
import com.example.sharpoclockserver.model.repository.WatchRepository
import com.example.sharpoclockserver.model.snapshot.SemanticWatchSearchStore
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class WatchService(
    private val watchRepository: WatchRepository,
    private val vectorStore: VectorStore,
    private val semanticWatchSearchStore: SemanticWatchSearchStore
) {

    fun getAllWatchesPaged(
        page: Int,
        size: Int,
    ): ResponseEntity<PagedModel<WatchController.WatchForDisplay>> {

        val pageable = PageRequest.of(page, size)

        val watchesOrderedByCreatedAtDescPaged = watchRepository.findAllByOrderByCreatedAtDesc(pageable).map { watch ->
            watch.toWatchForDisplay()
        }

        return ResponseEntity(
            PagedModel(
                watchesOrderedByCreatedAtDescPaged
            ),
            HttpStatus.OK
        )
    }

    fun getWatchDetailsById(
        watchId: UUID,
    ): ResponseEntity<Optional<WatchController.WatchForDisplay>> {
        val watch = watchRepository.findById(watchId).orElse(null) ?: return ResponseEntity(
            null,
            HttpStatus.NOT_FOUND
        )

        return ResponseEntity(
            Optional.of(watch.toWatchForDisplay()),
            HttpStatus.OK
        )
    }

    fun initSemanticSearchPagedAndCacheResults(
        query: String,
        size: Int,
    ): ResponseEntity<SemanticWatchSearchPage> {

        val documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(100)
                .similarityThreshold(.6)
                .build()
        )

        val orderedWatchIds = documents
            .map { document -> UUID.fromString(document.metadata["watchId"].toString()) }
            .distinct()

        val searchId = semanticWatchSearchStore.save(orderedWatchIds)

        return ResponseEntity(
            page(
                searchId = searchId,
                orderedWatchIds = orderedWatchIds,
                page = 0,
                size = size
            ),
            HttpStatus.OK
        )
    }

    fun getSemanticSearchResultsByIdPaged(
        searchId: UUID,
        page: Int,
        size: Int,
    ): ResponseEntity<SemanticWatchSearchPage> {

        val orderedWatchIds = semanticWatchSearchStore.get(searchId)

        return ResponseEntity(
            page(
                searchId = searchId,
                orderedWatchIds = orderedWatchIds,
                page = page,
                size = size
            ),
            HttpStatus.OK
        )
    }

    private fun page(
        searchId: UUID,
        orderedWatchIds: List<UUID>,
        page: Int,
        size: Int,
    ): SemanticWatchSearchPage {

        val currentPageOrderedWatchIds = orderedWatchIds
            .drop(page * size)
            .take(size)

        val watchesByIds = watchRepository
            .findAllById(currentPageOrderedWatchIds)
            .associateBy { it.id }

        return SemanticWatchSearchPage(
            searchId = searchId,
            content = orderedWatchIds
                .mapNotNull(watchesByIds::get)
                .map { it.toWatchForDisplay() },
            page = PageMetadata(
                number = page,
                size = size,
                totalElements = orderedWatchIds.size.toLong(),
                totalPages = (orderedWatchIds.size + size - 1) / size
            )
        )
    }

    private fun Watch.toWatchForDisplay() = WatchController.WatchForDisplay(
        id = this.id,

        thumbnailUrl = this.thumbnailUrl,
        thumbnailPublicId = this.thumbnailPublicId,
        heroUrl = this.heroUrl,
        heroPublicId = this.heroPublicId,

        brand = this.brand,
        name = this.name,
        description = this.description,
        sizes = this.sizes,
        price = this.price,
        unitsSold = this.unitsSold,

        isOOS = this.isOOS,
        isDiscounted = this.isDiscounted,
        isNew = this.isNew,
        isPopular = this.isPopular,

        discountPercentage = this.discountPercentage
    )
}