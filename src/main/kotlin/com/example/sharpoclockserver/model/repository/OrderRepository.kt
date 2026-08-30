package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.Order
import com.example.sharpoclockserver.model.OrderStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findAllByCustomerIdOrderByCreatedAtDesc(customerId: UUID): List<Order>

    fun findByIdAndCustomerId(
        id: UUID,
        customerId: UUID,
    ): Order?

    fun findByCustomerIdAndSourceCartIdAndStatus(
        customerId: UUID,
        sourceCartId: UUID,
        status: OrderStatus,
    ): Order?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from Order orders where orders.id = :orderId and orders.customer.id = :customerId")
    fun findOwnedByIdForUpdate(
        @Param("orderId") orderId: UUID,
        @Param("customerId") customerId: UUID,
    ): Order?
}