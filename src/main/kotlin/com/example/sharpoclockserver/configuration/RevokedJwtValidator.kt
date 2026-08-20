package com.example.sharpoclockserver.configuration

import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

class RevokedJwtValidator(
    private val revokedAccessTokenStore: RevokedAccessTokenStore
) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val jti = token.id ?: return OAuth2TokenValidatorResult.failure(
            OAuth2Error("invalid-token", "Token is missing jti claim", null)
        )

        val revoked = try {
            revokedAccessTokenStore.isRevoked(jti)
        } catch (e: RedisConnectionFailureException) {
            return OAuth2TokenValidatorResult.failure(
                OAuth2Error(
                    "revoked-token",
                    "Token validation is temporarily unavailable: ${e.localizedMessage}",
                    null
                )
            )
        }

        return if (revoked) {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error(
                    "revoked-token",
                    "Invalid access token",
                    null
                )
            )
        } else OAuth2TokenValidatorResult.success()
    }
}