package com.example.sharpoclockserver.auth

import com.example.sharpoclockserver.model.RegisteredDevice
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

// POST http://localhost:8080/auth/sign-in
// POST http://localhost:8080/auth/sign-up
// POST http://localhost:8080/auth/sign-out
// POST http://localhost:8080/auth/refresh

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    data class SignInRequest(
        val identifier: String,
        val password: String,
        val lastSignedInAt: Instant = Instant.now(),
        val lastSignedInDevice: RegisteredDevice? = null,
    )

    data class SignUpRequest(
        val email: String,
        val fullName: String,
        val pseudo: String,
        val password: String,
        /**
         * for a recommended search by age too
         * */
        val age: Int? = null,
        val phoneNumber: String? = null,
        val addressLine: String? = null,
        val joinedAt: Instant = Instant.now(),
        val joinedDevice: RegisteredDevice? = null,
    )

    data class RefreshRequest(
        val refreshToken: String,
    )

    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val tokenType: String,
        val expiresIn: Long,
        val refreshExpiresAt: Instant,
    )

    @PostMapping("/sign-in")
    fun signIn(
        @RequestBody signInRequest: SignInRequest
    ) = authService.signIn(signInRequest)

    @PostMapping("/sign-up")
    fun signUp(
        @RequestBody signUpRequest: SignUpRequest
    ) = authService.createCustomer(signUpRequest)

    @PostMapping("/sign-out")
    fun signOut(
        @RequestBody signOutRequest: RefreshRequest
    ) = authService.signOut(signOutRequest)

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody refreshRequest: RefreshRequest
    ) = authService.refresh(refreshRequest)
}