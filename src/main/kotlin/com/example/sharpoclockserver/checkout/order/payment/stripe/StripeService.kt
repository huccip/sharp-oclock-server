package com.example.sharpoclockserver.checkout.order.payment.stripe

import com.example.sharpoclockserver.model.repository.OrderRepository
import org.springframework.stereotype.Service

@Service
class StripeService(
    private val orderRepository: OrderRepository,
) {
}