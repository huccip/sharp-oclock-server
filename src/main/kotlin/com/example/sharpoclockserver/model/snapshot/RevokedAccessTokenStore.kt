package com.example.sharpoclockserver.model.snapshot

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RevokedAccessTokenStore(
    private val redisTemplate: StringRedisTemplate
) {
    fun revoke(jti: String, ttl: Duration) {
        if (!ttl.isNegative && !ttl.isZero) {
            redisTemplate.opsForValue().set(
                "revoked:$jti",
                "1",
                ttl
            )
        }
    }

    fun isRevoked(jti: String): Boolean = redisTemplate.hasKey("revoked:$jti")
}