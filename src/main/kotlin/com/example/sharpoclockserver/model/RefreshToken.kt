package com.example.sharpoclockserver.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(
            name = "idx_refresh_token_session",
            columnList = "session_id",
        ),
        Index(
            name = "idx_refresh_token_customer_device",
            columnList = "customer_id, device_id",
        ),
    ],
)
data class RefreshToken(
    @Column(name = "token_hash", length = 64, unique = true, nullable = false, updatable = false)
    val tokenHash: String,
    @Column(name = "issued_at", nullable = false, updatable = false)
    val issuedAt: Instant,
    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,
    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "session_id", columnDefinition = "uuid", nullable = false, updatable = false)
    val sessionId: UUID,

    @Column(name = "device_id", nullable = false, updatable = false)
    val deviceId: String,

    @Column(name = "customer_id", columnDefinition = "uuid", nullable = false, updatable = false)
    val customerId: UUID,

    @Id @Column(nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    val id: UUID,
)
