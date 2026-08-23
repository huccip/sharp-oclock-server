package com.example.sharpoclockserver.product

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.sharpoclockserver.model.Watch
import com.example.sharpoclockserver.model.repository.WatchRepository
import jakarta.transaction.Transactional
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class WatchService(
    private val watchRepository: WatchRepository,
    @Qualifier("adminListingAssistantChatClient")
    private val chatClient: ChatClient,
    private val cloudinary: Cloudinary
) {

    private data class UploadedImage(
        val url: String,
        val publicId: String,
    )

    fun generateDetails(
        prompt: String,
    ): AdminWatchController.WatchDetails =
        chatClient.prompt()
            .user(prompt)
            .call()
            .entity(AdminWatchController.WatchDetails::class.java)
            ?: error("Couldn't create AdminWatchController for prompt $prompt")

    private fun uploadImage(
        file: MultipartFile,
    ): UploadedImage {
        require(!file.isEmpty) { "Image file can't be empty" }

        val allowedTypes = setOf(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp",
        )
        require(file.contentType in allowedTypes) { "Unsupported image type" }

        val result = cloudinary.uploader()
            .upload(
                file.bytes,
                ObjectUtils.asMap(
                    "folder", "sharp-oclock-server/watches/images",
                    "resource_type", "image"
                )
            )

        return UploadedImage(
            url = result["secure_url"] as? String
                ?: error("Cloudinary did not return a secure URL"),
            publicId = result["public_id"] as? String
                ?: error("Cloudinary did not return a public ID"),
        )
    }

    fun saveWatch(
        saveWatchRequest: AdminWatchController.SaveWatchRequest
    ) {
        val thumbnailFile = saveWatchRequest.thumbnailImage
            ?.takeUnless { it.isEmpty }
            ?: error("Thumbnail image is required")
        val heroFile = saveWatchRequest.heroImage
            ?.takeUnless { it.isEmpty }
            ?: error("Hero image is required")

        val uploadedThumbnail = uploadImage(thumbnailFile)
        val uploadedHero = uploadImage(heroFile)

        val newWatch = Watch(
            id = UUID.randomUUID(),
            thumbnailUrl = uploadedThumbnail.url,
            thumbnailPublicId = uploadedThumbnail.publicId,
            heroUrl = uploadedHero.url,
            heroPublicId = uploadedHero.publicId,
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
        val watch = getWatchById(watchId)

        watchRepository.delete(watch)
        cloudinary.uploader().destroy(watch.thumbnailPublicId, ObjectUtils.emptyMap())
        cloudinary.uploader().destroy(watch.heroPublicId, ObjectUtils.emptyMap())
    }

    @Transactional
    fun updateWatchById(watchId: UUID, editWatchRequest: AdminWatchController.SaveWatchRequest) {
        val watchToEdit = getWatchById(watchId)
        val uploadedThumbnail = editWatchRequest.thumbnailImage
            ?.takeUnless { it.isEmpty }
            ?.let(::uploadImage)
        val uploadedHero = editWatchRequest.heroImage
            ?.takeUnless { it.isEmpty }
            ?.let(::uploadImage)

        val editedWatch = watchToEdit.copy(
            thumbnailUrl = uploadedThumbnail?.url ?: watchToEdit.thumbnailUrl,
            thumbnailPublicId = uploadedThumbnail?.publicId ?: watchToEdit.thumbnailPublicId,
            heroUrl = uploadedHero?.url ?: watchToEdit.heroUrl,
            heroPublicId = uploadedHero?.publicId ?: watchToEdit.heroPublicId,
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

        uploadedThumbnail?.let {
            cloudinary.uploader().destroy(watchToEdit.thumbnailPublicId, ObjectUtils.emptyMap())
        }
        uploadedHero?.let {
            cloudinary.uploader().destroy(watchToEdit.heroPublicId, ObjectUtils.emptyMap())
        }
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
            heroUrl = watch.heroUrl,
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
            heroUrl = watch.heroUrl,
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
