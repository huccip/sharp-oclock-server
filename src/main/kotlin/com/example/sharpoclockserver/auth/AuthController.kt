package com.example.sharpoclockserver.auth

import com.example.sharpoclockserver.configuration.Identifier
import com.example.sharpoclockserver.model.RegisteredDevice
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        @NotNull(message = "E-mail/Username is required")
        @Identifier
        val identifier: String,
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        val password: String,
        @NotNull(message = "This field is required")
        val lastSignedInDevice: RegisteredDevice,
    )

    data class SignUpRequest(
        @NotNull(message = "E-mail is required")
        @Email(message = "Please enter a valid email address")
        val email: String,
        @NotNull(message = "Full Name is required")
        @Size(min = 5, max = 30)
        val fullName: String,
        @NotNull(message = "Username is required")
        @Pattern(
            regexp = "^(?=.{5,30}$)[a-zA-Z0-9]+(?:[._-][a-zA-Z0-9]+)*$",
            message = "Please enter a valid username"
        )
        val pseudo: String,
        @NotNull(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        val password: String,
        /**
         * for a recommended search by age too
         * */
        @Min(17, message = "Your age must be at least 17")
        val age: Int? = null,
        @Pattern(
            regexp = "^(0|212|\\+212)([67])[0-9]{8}$",
            message = "Please enter a valid Moroccan phone number"
        )
        val phoneNumber: String? = null,
        val addressLine: String? = null,
        @NotNull(message = "This field is required")
        val joinedDevice: RegisteredDevice
    )

    data class RefreshRequest(
        @NotNull(message = "This field is required")
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
        @NotNull(message = "E-mail/Username is required")
        @Identifier
        val identifier: String,
    )

    data class PasswordResetConfirmRequest(
        @NotNull(message = "E-mail/Username is required")
        @Identifier
        val identifier: String,
        @NotNull(message = "OTP code is required")
        @Pattern(
            regexp = "^[0-9]{6}$"
        )
        val otp: String,
        @NotNull(message = "New Password is required")
        @Size(min = 8, max = 64, message = "New Password must be between 8 and 64 characters")
        val newPassword: String
    )

    @PostMapping("/sign-in")
    fun signIn(
        @RequestBody @Valid signInRequest: SignInRequest
    ) = authService.signIn(signInRequest)

    @PostMapping("/sign-up")
    fun signUp(
        @RequestBody @Valid signUpRequest: SignUpRequest
    ) = authService.createCustomer(signUpRequest)

    @PostMapping("/sign-out")
    fun signOut(
        @AuthenticationPrincipal accessToken: Jwt,
        @RequestBody @Valid signOutRequest: RefreshRequest
    ) = authService.signOut(accessToken, signOutRequest)

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody @Valid refreshRequest: RefreshRequest
    ) = authService.refresh(refreshRequest)

    // will send OTP in the inbox (if the identifier exists)
    @PostMapping("/password-reset/request")
    fun requestPasswordReset(
        @RequestBody @Valid passwordResetRequest: PasswordResetRequest
    ) = authService.requestPasswordReset(passwordResetRequest)

    // will validate the OTP and sets the new password and signs the customer out everywhere
    @PostMapping("/password-reset/confirm")
    fun confirmPasswordReset(
        @RequestBody @Valid passwordResetConfirmRequest: PasswordResetConfirmRequest
    ) = authService.confirmPasswordReset(passwordResetConfirmRequest)
}