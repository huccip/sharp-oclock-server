package com.example.sharpoclockserver.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun jwtSecret(
        @Value($$"${app.jwt.secret}")
        encodedSecret: String
    ): SecretKey {
        val keyBytes = Base64.getDecoder().decode(encodedSecret)

        require(keyBytes.size >= 32) {
            "JWT secret must contain at least 256 bits"
        }

        return SecretKeySpec(keyBytes, "HmacSHA256")
    }

    @Bean
    fun jwtDecoder(
        secretKey: SecretKey,
        @Value($$"${app.jwt.issuer}")
        issuer: String,
        @Value($$"${app.jwt.audience}")
        audience: String,
        @Value($$"${app.jwt.client-id}")
        clientId: String,
    ): JwtDecoder {

        val decoder = NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .validateType(false)
            .build()

        decoder.setJwtValidator(
            JwtValidators.createAtJwtValidator()
                .issuer(issuer)
                .audience(audience)
                .clientId(clientId)
                .build()
        )

        return decoder
    }

    @Bean
    fun jwtEncoder(
        secretKey: SecretKey
    ): JwtEncoder =
        NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { httpRequest ->
                httpRequest
                    .requestMatchers(
                        HttpMethod.POST,
                        "/auth/**"
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { }
            }
            .build()
}