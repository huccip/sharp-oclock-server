package com.example.sharpoclockserver.model

import com.example.sharpoclockserver.product.WatchController.WatchForDisplay
import java.util.*

data class SemanticWatchSearchPage(
    val searchId: UUID,
    val content: List<WatchForDisplay>,
    val page: PageMetadata
)

data class PageMetadata(
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
