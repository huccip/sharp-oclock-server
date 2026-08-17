package com.example.sharpoclockserver.auth

import com.example.sharpoclockserver.model.Customer
import com.example.sharpoclockserver.model.RefreshToken
import com.example.sharpoclockserver.model.repository.CustomerRepository
import com.example.sharpoclockserver.model.repository.RefreshTokenRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val customerRepository: CustomerRepository,
    private val refreshTokenRepository: RefreshTokenRepository,

    private val passwordEncoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
    @Value($$"${app.jwt.issuer}")
    private val jwtIssuer: String,
    @Value($$"${app.jwt.audience}")
    private val jwtAudience: String,
    @Value($$"${app.jwt.client-id}")
    private val jwtClientId: String,
    @Value($$"${app.jwt.access-token-expiry-seconds}")
    private val accessTokenExpirySeconds: Long,
    @Value($$"${app.jwt.refresh-token-expiry-seconds}")
    private val refreshTokenExpirySeconds: Long,

    private val mailSender: JavaMailSender,
    @Value($$"${app.mail.from}")
    private val mailFrom: String,
) {

    private val secureRandom = SecureRandom()

    private fun sendEmail(
        recipientMailAddress: String,
        subject: String,
        body: String,
    ) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, StandardCharsets.UTF_8.name())

        helper.setFrom(mailFrom)
        helper.setTo(recipientMailAddress)
        helper.setSubject(subject)
        helper.setText(body, true)

        mailSender.send(message)
    }

    @Transactional
    fun createCustomer(
        signUpRequest: AuthController.SignUpRequest
    ): ResponseEntity<*> {
        if (customerRepository.findByEmailOrPseudo(
                signUpRequest.email,
                signUpRequest.pseudo
            ) != null
        ) return ResponseEntity(
            "Customer already exists", HttpStatus.CONFLICT
        )

        val hashedPassword = passwordEncoder.encode(signUpRequest.password) ?: return ResponseEntity(
            "Could not hash the password", HttpStatus.BAD_REQUEST
        )

        val customer = Customer(
            id = UUID.randomUUID(),
            email = signUpRequest.email,
            fullName = signUpRequest.fullName,
            pseudo = signUpRequest.pseudo,
            password = hashedPassword,

            age = signUpRequest.age,
            addressLine = signUpRequest.addressLine,

            lastSignedInAtList = listOf(Instant.now()),
            lastSignedInDeviceList = signUpRequest.joinedDevice?.let(::listOf) ?: emptyList(),
        )

        customerRepository.save(customer)

        try {
            sendEmail(
                signUpRequest.email,
                "Welcome to Sharp O\'clock",
                """
                <!doctype html>
                <html>
                  <head>
                    <meta http-equiv=3D"Content-Type" content=3D"text/html; charset=3DUTF-8">
                  </head>
                  <body style=3D"font-family: sans-serif;">
                Dear <b>${signUpRequest.fullName}</b>,
                <br/>
                Welcome to our shop! Enjoy your stay.
                <br/>
                The below block is information regarding your account:
                <br/>
                <ul>
                <li>Full name: ${customer.fullName}</li>
                <li>Pseudo: ${customer.pseudo}</li>
                <li>Email: ${customer.email}</li>
                <li>Address: ${customer.addressLine}</li>
                <li>Age: ${customer.age}</li>
                <li>Last connected at: ${
                    customer.lastSignedInAtList.joinToString(", ") {
                        SimpleDateFormat(
                            "dd/MM/yyyy @HH:mm a",
                            Locale.getDefault()
                        ).format(Date.from(it))
                    }
                }</li>
                <li>Last joined from: ${
                    customer.lastSignedInDeviceList.joinToString(", ") {
                        it.toString()
                    }
                }
                </li>
                </ul>
                <br/>
                Best Regards,
                <br/>
                <i>Sharp O'Clock Dev Team.</i>.
                </body>
                </html>
            """.trimIndent()
            )
        } catch (e: MailSendException) {
            return ResponseEntity(
                "Could not send a welcome e-mail to ${customer.id}/${customer.email}: ${e.message}",
                HttpStatus.CONFLICT
            )
        }

        return ResponseEntity(
            "Customer successfully created!",
            HttpStatus.CREATED
        )
    }

    @Transactional
    fun signIn(
        signInRequest: AuthController.SignInRequest
    ): ResponseEntity<*> {
        val device = signInRequest.lastSignedInDevice ?: return ResponseEntity(
            "Device parameters are required",
            HttpStatus.BAD_REQUEST
        )
        if (device.deviceId.isBlank()) return ResponseEntity(
            "Device id must be attached",
            HttpStatus.BAD_REQUEST
        )
        val customer = customerRepository.findByEmailOrPseudo(signInRequest.identifier, signInRequest.identifier)
            ?: return invalidCredentials()

        if (!passwordEncoder.matches(signInRequest.password, customer.password))
            return invalidCredentials()

        refreshTokenRepository.revokeSessionsForDevice(
            customerId = customer.id,
            deviceId = device.deviceId,
            revokedAt = Instant.now(),
        )

        val refreshExpiresAt =
            Instant.now().plusSeconds(refreshTokenExpirySeconds)

        return ResponseEntity(
            issueTokenPair(
                customerId = customer.id,
                deviceId = device.deviceId,
                sessionId = UUID.randomUUID(),
                refreshExpiresAt = refreshExpiresAt,
                issuedAt = Instant.now(),
            ),
            HttpStatus.OK
        )
    }

    @Transactional
    fun signOut(
        request: AuthController.RefreshRequest,
    ): ResponseEntity<*> {
        val parsedToken = parseRefreshToken(request.refreshToken)
            ?: return invalidRefreshToken()

        val storedToken =
            refreshTokenRepository.findByIdForUpdate(parsedToken.id)
                ?: return invalidRefreshToken()

        if (tokenHashMatches(storedToken.tokenHash, parsedToken.secret)) {
            refreshTokenRepository.revokeSession(
                sessionId = storedToken.sessionId,
                revokedAt = Instant.now(),
            )
        }

        return ResponseEntity(
            "Customer successfully signed out!",
            HttpStatus.OK
        )
    }

    @Transactional
    fun refresh(
        request: AuthController.RefreshRequest,
    ): ResponseEntity<*> {
        val parsedToken = parseRefreshToken(request.refreshToken)
            ?: return invalidRefreshToken()

        val storedToken =
            refreshTokenRepository.findByIdForUpdate(parsedToken.id)
                ?: return invalidRefreshToken()

        if (!tokenHashMatches(storedToken.tokenHash, parsedToken.secret)) {
            return invalidRefreshToken()
        }

        val now = Instant.now()

        /**
         * a consumed token being presented again indicates replay
         * revoke every refresh token belonging to that device session
         * */
        if (storedToken.consumedAt != null) {
            refreshTokenRepository.revokeSession(
                sessionId = storedToken.sessionId,
                revokedAt = now,
            )

            return invalidRefreshToken()
        }

        if (storedToken.revokedAt != null) {
            return invalidRefreshToken()
        }

        if (!storedToken.expiresAt.isAfter(now)) {
            storedToken.revokedAt = now
            refreshTokenRepository.save(storedToken)

            return invalidRefreshToken()
        }

        if (!customerRepository.existsById(storedToken.customerId)) {
            refreshTokenRepository.revokeSession(
                sessionId = storedToken.sessionId,
                revokedAt = now,
            )

            return invalidRefreshToken()
        }

        storedToken.consumedAt = now
        refreshTokenRepository.save(storedToken)

        return ResponseEntity(
            issueTokenPair(
                customerId = storedToken.customerId,
                deviceId = storedToken.deviceId,
                sessionId = storedToken.sessionId,
                refreshExpiresAt = storedToken.expiresAt,
                issuedAt = now,
            ),
            HttpStatus.OK
        )
    }

    fun resetPassword(
        resetPasswordRequest: AuthController.ResetPasswordRequest
    ): ResponseEntity<*> {
        //TODO
        return ResponseEntity.ok().body("placeholder")
    }

    private fun invalidRefreshToken(): ResponseEntity<*> =
        ResponseEntity(
            "Invalid refresh token",
            HttpStatus.UNAUTHORIZED
        )

    private fun invalidCredentials(): ResponseEntity<String> =
        ResponseEntity(
            "Invalid credentials",
            HttpStatus.UNAUTHORIZED
        )

    private fun issueTokenPair(
        customerId: UUID,
        deviceId: String,
        sessionId: UUID,
        refreshExpiresAt: Instant,
        issuedAt: Instant,
    ): AuthController.TokenResponse {
        val accessToken = createAccessToken(
            customerId = customerId,
            issuedAt = issuedAt,
        )

        val refreshToken = createRefreshToken(
            customerId = customerId,
            deviceId = deviceId,
            sessionId = sessionId,
            issuedAt = issuedAt,
            expiresAt = refreshExpiresAt,
        )

        return AuthController.TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = accessTokenExpirySeconds,
            refreshExpiresAt = refreshExpiresAt,
        )
    }

    private fun createAccessToken(
        customerId: UUID,
        issuedAt: Instant,
    ): String {
        val header = JwsHeader
            .with(MacAlgorithm.HS256)
            .type("at+jwt")
            .build()

        val claims = JwtClaimsSet.builder()
            .issuer(jwtIssuer)
            .subject(customerId.toString())
            .audience(listOf(jwtAudience))
            .issuedAt(issuedAt)
            .expiresAt(
                issuedAt.plusSeconds(accessTokenExpirySeconds)
            )
            .id(UUID.randomUUID().toString())
            .claim("client_id", jwtClientId)
            .build()

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).tokenValue
    }

    private fun createRefreshToken(
        customerId: UUID,
        deviceId: String,
        sessionId: UUID,
        issuedAt: Instant,
        expiresAt: Instant,
    ): String {
        val tokenId = UUID.randomUUID()
        val secret = ByteArray(REFRESH_TOKEN_SECRET_BYTES)

        secureRandom.nextBytes(secret)

        refreshTokenRepository.save(
            RefreshToken(
                id = tokenId,
                sessionId = sessionId,
                customerId = customerId,
                deviceId = deviceId,
                tokenHash = hashTokenSecret(secret),
                issuedAt = issuedAt,
                expiresAt = expiresAt,
            )
        )

        val encodedSecret = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(secret)

        return "$tokenId.$encodedSecret"
    }

    private fun hashTokenSecret(secret: ByteArray): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(secret)

        return HexFormat.of().formatHex(digest)
    }

    private fun parseRefreshToken(
        token: String,
    ): ParsedRefreshToken? {
        val separator = token.indexOf('.')

        if (
            separator <= 0 ||
            separator == token.lastIndex ||
            token.indexOf('.', separator + 1) >= 0
        ) {
            return null
        }

        val id = runCatching {
            UUID.fromString(token.substring(0, separator))
        }.getOrNull() ?: return null

        val secret = runCatching {
            Base64.getUrlDecoder().decode(
                token.substring(separator + 1)
            )
        }.getOrNull() ?: return null

        if (secret.size != REFRESH_TOKEN_SECRET_BYTES) {
            return null
        }

        return ParsedRefreshToken(id, secret)
    }

    private fun tokenHashMatches(
        storedHash: String,
        secret: ByteArray,
    ): Boolean {
        val presentedHash = hashTokenSecret(secret)

        return MessageDigest.isEqual(
            storedHash.toByteArray(StandardCharsets.US_ASCII),
            presentedHash.toByteArray(StandardCharsets.US_ASCII),
        )
    }

    private data class ParsedRefreshToken(
        val id: UUID,
        val secret: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParsedRefreshToken

            if (id != other.id) return false
            if (!secret.contentEquals(other.secret)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + secret.contentHashCode()
            return result
        }
    }

    private companion object {
        const val REFRESH_TOKEN_SECRET_BYTES = 32
    }
}