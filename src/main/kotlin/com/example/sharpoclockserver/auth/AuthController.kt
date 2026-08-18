package com.example.sharpoclockserver.auth

import com.example.sharpoclockserver.model.RegisteredDevice
import org.springframework.web.bind.annotation.*
import java.time.Instant

// POST http://localhost:8080/auth/sign-in
// POST http://localhost:8080/auth/sign-up
// POST http://localhost:8080/auth/sign-out
// POST http://localhost:8080/auth/refresh
// POST http://localhost:8080/auth/password-reset/request
// POST http://localhost:8080/auth/password-reset/confirm

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    data class SignInRequest(
        val identifier: String,
        val password: String,
        val lastSignedInDevice: RegisteredDevice,
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
        val joinedDevice: RegisteredDevice
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

    data class PasswordResetRequest(
        val identifier: String,
    )

    data class PasswordResetConfirmRequest(
        val identifier: String,
        val otp: String,
        val newPassword: String
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

    // will send OTP in the inbox (if the identifier exists)
    @PostMapping("/password-reset/request")
    fun requestPasswordReset(
        @RequestBody passwordResetRequest: PasswordResetRequest
    ) = authService.requestPasswordReset(passwordResetRequest)

    // will validate the OTP and sets the new password and signs the customer out everywhere
    @PostMapping("/password-reset/confirm")
    fun confirmPasswordReset(
        @RequestBody passwordResetConfirmRequest: PasswordResetConfirmRequest
    ) = authService.confirmPasswordReset(passwordResetConfirmRequest)
}