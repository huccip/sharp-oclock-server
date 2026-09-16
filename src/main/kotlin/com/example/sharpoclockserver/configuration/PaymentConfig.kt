package com.example.sharpoclockserver.configuration

import com.paypal.sdk.Environment
import com.paypal.sdk.PaypalServerSdkClient
import com.paypal.sdk.authentication.ClientCredentialsAuthModel
import com.stripe.StripeClient
import org.slf4j.event.Level
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentConfig {

    @Bean
    fun initPaypal(
        @Value($$"${paypal.client.app}")
        clientId: String,
        @Value($$"${paypal.client.secret}")
        clientSecret: String,
        @Value($$"$paypal.mode")
        mode: String,
    ): PaypalServerSdkClient = PaypalServerSdkClient.Builder()
        .loggingConfig { loggingConfigBuilder ->
            loggingConfigBuilder.level(Level.DEBUG)
            loggingConfigBuilder.requestConfig { requestLoggingConfigBuilder -> requestLoggingConfigBuilder.body(true) }
            loggingConfigBuilder.responseConfig { responseLoggingConfigBuilder ->
                responseLoggingConfigBuilder.headers(
                    true
                )
            }
        }
        .httpClientConfig { httpClientConfigBuilder ->
            httpClientConfigBuilder.timeout(0)
        }
        .clientCredentialsAuth(
            ClientCredentialsAuthModel
                .Builder(
                    clientId,
                    clientSecret
                )
                .build()
        )
        .environment(Environment.valueOf(mode))
        .build()

    @Bean
    fun initStripe(
        @Value($$"${stripe.api-key}")
        apiKey: String,
    ): StripeClient = StripeClient(apiKey)
}