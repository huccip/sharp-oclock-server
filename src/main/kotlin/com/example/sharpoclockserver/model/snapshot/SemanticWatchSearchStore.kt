package com.example.sharpoclockserver.model.snapshot

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class SemanticWatchSearchStore(
    private val redisTemplate: StringRedisTemplate
) {
    private val ttl = Duration.ofMinutes(10)

    fun save(
        orderedWatchIds: List<UUID>,
    ): UUID {
        val searchId = UUID.randomUUID()

        redisTemplate.opsForValue().set(
            "semantic-watch-search:$searchId",
            orderedWatchIds.joinToString(","),
            ttl
        )

        return searchId
    }

    fun get(
        searchId: UUID,
    ): List<UUID> {
        val result = redisTemplate.opsForValue().get("semantic-watch-search:$searchId")
            ?: error("Search could not be loaded or has been expired")

        if (result.isBlank()) return emptyList()

        return result.split(",").map(UUID::fromString)
    }
}