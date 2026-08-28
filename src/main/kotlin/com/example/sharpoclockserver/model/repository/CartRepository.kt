package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.Cart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CartRepository : JpaRepository<Cart, UUID> {
    fun findAllByCustomerId(customerId: UUID): List<Cart>
    fun deleteAllByCustomerId(customerId: UUID)
}