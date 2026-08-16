package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.RefreshToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository: JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT token
        FROM RefreshToken token
        WHERE token.id = :id
        """
    )
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): RefreshToken?

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true,
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
        flushAutomatically = true,
        clearAutomatically = true,
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
}