package com.example.sharpoclockserver.configuration

import com.stripe.StripeClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StripeConfig {

    @Bean
    fun initStripe(
        @Value($$"${stripe.api-key}")
        apiKey: String,
    ): StripeClient = StripeClient(apiKey)
}