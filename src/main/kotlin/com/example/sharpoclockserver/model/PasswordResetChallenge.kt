package com.example.sharpoclockserver.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "password_reset_challenges")
data class PasswordResetChallenge(
    @Id
    @Column(columnDefinition = "uuid", nullable = false, unique = true, updatable = false)
    val id: UUID,

    @Column(name = "otp_hash", nullable = false)
    var otpHash: String, // b-crypted
    @Column(name = "otp_expiry_at", nullable = false)
    var otpExpiryAt: Instant, // capped at now + 10 minutes
    @Column(name = "otp_sent_at", nullable = false)
    var otpSentAt: Instant,
    @Column(name = "otp_failed_attempts", nullable = false)
    var otpFailedAttempts: Int, // capped at 5

    @Column(name = "customer_id", columnDefinition = "uuid", nullable = false, updatable = false)
    val customerId: UUID,
)
