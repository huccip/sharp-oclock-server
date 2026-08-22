package com.example.sharpoclockserver.product

import com.example.sharpoclockserver.model.Watch
import com.example.sharpoclockserver.model.repository.WatchRepository
import jakarta.transaction.Transactional
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class WatchService(
    private val watchRepository: WatchRepository,
    @Qualifier("adminListingAssistantChatClient")
    private val chatClient: ChatClient
) {

    fun generateDetails(
        prompt: String,
    ): AdminWatchController.WatchDetails =
        chatClient.prompt()
            .user(prompt)
            .call()
            .entity(AdminWatchController.WatchDetails::class.java)
            ?: error("Couldn't create AdminWatchController for prompt $prompt")

    fun saveWatch(
        saveWatchRequest: AdminWatchController.SaveWatchRequest
    ) {
        val newWatch = Watch(
            id = UUID.randomUUID(),
            thumbnailUrl = saveWatchRequest.thumbnailUrl,
            imageUrl = saveWatchRequest.imageUrl,
            brand = saveWatchRequest.brand,
            name = saveWatchRequest.name,
            description = saveWatchRequest.description,
            sizes = saveWatchRequest.sizes,
            price = saveWatchRequest.priceDisplay.multiply(BigDecimal(100)).toInt(),
            unitsSold = 0L,
            isOOS = saveWatchRequest.isOOS,
            isDiscounted = saveWatchRequest.isDiscounted,
            isNew = saveWatchRequest.isNew,
            isPopular = false,

            createdAt = Instant.now(),
        )

        watchRepository.save(newWatch)
    }

    fun listWatches(): List<Watch> = watchRepository.findAll()

    @Transactional
    fun deleteWatchById(watchId: UUID) {
        if (!watchRepository.existsById(watchId)) error("Watch was not found")

        watchRepository.deleteById(watchId)
    }

    @Transactional
    fun updateWatchById(watchId: UUID, editWatchRequest: AdminWatchController.SaveWatchRequest) {
        val watchToEdit = watchRepository.findById(watchId).orElse(null) ?: error("Watch was not found")

        val editedWatch = watchToEdit.copy(
            thumbnailUrl = editWatchRequest.thumbnailUrl,
            imageUrl = editWatchRequest.imageUrl,
            brand = editWatchRequest.brand,
            name = editWatchRequest.name,
            description = editWatchRequest.description,
            sizes = editWatchRequest.sizes,
            price = editWatchRequest.priceDisplay.multiply(BigDecimal(100)).toInt(),
            isOOS = editWatchRequest.isOOS,
            isDiscounted = editWatchRequest.isDiscounted,
            isNew = editWatchRequest.isNew,

            lastEditedAt = Instant.now()
        )

        watchRepository.save(editedWatch)
    }

    fun getWatchById(
        watchId: UUID,
    ): Watch = watchRepository.findById(watchId).orElse(null) ?: error("Watch was not found")

    fun getWatchForEdit(
        watchId: UUID,
    ): AdminWatchController.EditWatchView {
        val watch = getWatchById(watchId)

        return AdminWatchController.EditWatchView(
            thumbnailUrl = watch.thumbnailUrl,
            imageUrl = watch.imageUrl,
            brand = watch.brand,
            name = watch.name,
            description = watch.description,
            sizes = watch.sizes,
            priceDisplay = BigDecimal.valueOf(watch.price.toLong(), 2),
            isOOS = watch.isOOS,
            isDiscounted = watch.isDiscounted,
            isNew = watch.isNew,
        )
    }

    fun getWatchForDetails(
        watchId: UUID,
    ): AdminWatchController.WatchDetailsView {
        val watch = getWatchById(watchId)

        return AdminWatchController.WatchDetailsView(
            thumbnailUrl = watch.thumbnailUrl,
            imageUrl = watch.imageUrl,
            brand = watch.brand,
            name = watch.name,
            description = watch.description,
            sizes = watch.sizes,
            priceDisplay = BigDecimal.valueOf(watch.price.toLong(), 2),
            unitsSold = watch.unitsSold,
            isOOS = watch.isOOS,
            isDiscounted = watch.isDiscounted,
            isNew = watch.isNew,
            isPopular = watch.isPopular,
        )
    }
}
