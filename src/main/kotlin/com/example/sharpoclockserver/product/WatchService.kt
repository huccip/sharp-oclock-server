package com.example.sharpoclockserver.product

import com.example.sharpoclockserver.model.repository.WatchRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class WatchService(
    private val watchRepository: WatchRepository
) {

    fun getAllWatchesPaged(
        page: Int,
        size: Int,
    ): ResponseEntity<PagedModel<WatchController.WatchForDisplay>> {

        val pageable = PageRequest.of(page, size)

        val watchesOrderedByCreatedAtDescPaged = watchRepository.findAllByOrderByCreatedAtDesc(pageable).map { watch ->
            WatchController.WatchForDisplay(
                id = watch.id,

                thumbnailUrl = watch.thumbnailUrl,
                thumbnailPublicId = watch.thumbnailPublicId,
                heroUrl = watch.heroUrl,
                heroPublicId = watch.heroPublicId,

                brand = watch.brand,
                name = watch.name,
                description = watch.description,
                sizes = watch.sizes,
                price = watch.price,
                unitsSold = watch.unitsSold,

                isOOS = watch.isOOS,
                isDiscounted = watch.isDiscounted,
                isNew = watch.isNew,
                isPopular = watch.isPopular,

                discountPercentage = watch.discountPercentage
            )
        }

        return ResponseEntity(
            PagedModel(
                watchesOrderedByCreatedAtDescPaged
            ),
            HttpStatus.OK
        )
    }
}