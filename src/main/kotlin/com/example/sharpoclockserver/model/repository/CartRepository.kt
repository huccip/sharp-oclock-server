package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.Cart
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CartRepository : JpaRepository<Cart, UUID> {
    fun findAllByCustomerId(customerId: UUID): List<Cart>
    fun deleteAllByCustomerId(customerId: UUID)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.id = :cartId and cart.customer.id = :customerId")
    fun findOwnedByIdForUpdate(
        @Param("cartId") cartId: UUID,
        @Param("customerId") customerId: UUID,
    ): Cart?
}
