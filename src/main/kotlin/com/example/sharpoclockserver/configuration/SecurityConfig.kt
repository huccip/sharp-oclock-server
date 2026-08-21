package com.example.sharpoclockserver.configuration

import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.session.HttpSessionEventPublisher
import java.util.*
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

        revokedAccessTokenStore: RevokedAccessTokenStore
    ): JwtDecoder {

        val decoder = NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .validateType(false)
            .build()

        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createAtJwtValidator()
                    .issuer(issuer)
                    .audience(audience)
                    .clientId(clientId)
                    .build(),
                RevokedJwtValidator(revokedAccessTokenStore)
            )
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
    fun httpSessionEventPublisher() = HttpSessionEventPublisher()

    @Bean
    fun adminUserDetailsService(
        @Value($$"${app.admin.identifier}")
        identifier: String,
        @Value($$"${app.admin.password-hash}")
        password: String
    ): UserDetailsService =
        InMemoryUserDetailsManager(
            User.builder()
                .username(identifier)
                .password(password)
                .roles("ADMIN")
                .build()
        )

    @Bean
    @Order(1)
    fun adminFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests { httpRequest ->
                httpRequest
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/admin/sign-in")
                    .permitAll()
                    .anyRequest()
                    .hasRole("ADMIN")
            }
            .formLogin {
                it.loginPage("/admin/sign-in")
                it.loginProcessingUrl("/admin/sign-in")
                it.usernameParameter("identifier")
                it.passwordParameter("password")
                it.defaultSuccessUrl("/admin/dashboard", false)
                it.failureUrl("/admin/sign-in?error")
                it.permitAll()
            }
            .logout {
                it.logoutUrl("/admin/sign-out")
                it.logoutSuccessUrl("/admin/sign-in?sign-out")
                it.permitAll()
            }
            .sessionManagement {
                it.maximumSessions(1)
                    .expiredUrl("/admin/sign-in?expired")
            }
            .build()

    @Bean
    @Order(2)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { httpRequest ->
                httpRequest
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                    )
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/auth/sign-up",
                        "/auth/sign-in",
                        "/auth/refresh"
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