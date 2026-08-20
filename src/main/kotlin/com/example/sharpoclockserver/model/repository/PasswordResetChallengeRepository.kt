package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.PasswordResetChallenge
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PasswordResetChallengeRepository: JpaRepository<PasswordResetChallenge, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByCustomerId(customerId: UUID): PasswordResetChallenge?
}