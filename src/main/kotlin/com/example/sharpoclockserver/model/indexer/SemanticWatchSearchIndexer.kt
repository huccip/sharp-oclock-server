package com.example.sharpoclockserver.model.indexer

import com.example.sharpoclockserver.model.Watch
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import java.util.*

@Service
class SemanticWatchSearchIndexer(
    private val vectorStore: VectorStore
) {
    fun index(
        watch: Watch
    ) {
        val document = Document.builder()
            .id(watch.id.toString())
            .text(watch.toString())
            .metadata(
                "watchId",
                watch.id.toString()
            )
            .build()

        vectorStore.add(listOf(document))
    }

    fun delete(watchId: UUID) {
        vectorStore.delete(listOf(watchId.toString()))
    }
}