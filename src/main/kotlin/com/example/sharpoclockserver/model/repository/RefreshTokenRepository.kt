package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.RefreshToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepository: JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    override fun findById(
        @Param("id") id: UUID,
    ): Optional<RefreshToken>

    @Modifying(
        flushAutomatically = true
    )
    @Query(
        """
        UPDATE RefreshToken token
        SET token.revokedAt = :revokedAt
        WHERE token.sessionId = :sessionId
          AND token.revokedAt IS NULL
        """
    )
    fun revokeSession(
        @Param("sessionId") sessionId: UUID,
        @Param("revokedAt") revokedAt: Instant,
    ): Int

    @Modifying(
        flushAutomatically = true
    )
    @Query(
        """
        UPDATE RefreshToken token
        SET token.revokedAt = :revokedAt
        WHERE token.customerId = :customerId
          AND token.deviceId = :deviceId
          AND token.revokedAt IS NULL
        """
    )
    fun revokeSessionsForDevice(
        @Param("customerId") customerId: UUID,
        @Param("deviceId") deviceId: String,
        @Param("revokedAt") revokedAt: Instant,
    ): Int

    @Modifying(
        flushAutomatically = true
    )
    @Query(
        """
            UPDATE RefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.customerId = :customerId
                AND token.revokedAt IS NULL
        """
    )
    fun revokeSessionsForCustomer(
        @Param("customerId") customerId: UUID,
        @Param("revokedAt") revokedAt: Instant,
    ): Int
}