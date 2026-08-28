package com.example.sharpoclockserver.checkout

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/checkout/carts")
class CartController(
    private val cartService: CartService
) {

    data class CartLineRequest(
        @NotNull
        val watchId: UUID,
        @NotNull
        @NotBlank
        @Min(150)
        @Max(200)
        val watchSize: Int,
        @NotNull
        @Min(1)
        @Max(10)
        val quantity: Int,
    )

    @GetMapping
    fun getAllCartsByCustomerId(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<*> = cartService.getAllCartsByCustomerId(jwt)

    @GetMapping("/{cartId}")
    fun getCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID
    ): ResponseEntity<*> = cartService.getCartById(jwt = jwt, cartId = cartId)

    @PostMapping("/{cartId}/add")
    fun addCartLineToCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID,
        @RequestBody @Valid cartLineRequest: CartLineRequest
    ): ResponseEntity<*> = cartService.updateCartLineInCartById(
        jwt = jwt,
        cartId = cartId,
        cartLineRequest = cartLineRequest
    )

    // make sure to implement a request debouncing in the client-side
    @PostMapping("/{cartId}/update")
    fun editCarLineInCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID,
        @RequestBody @Valid cartLineRequest: CartLineRequest
    ): ResponseEntity<*> = cartService.updateCartLineInCartById(
        jwt = jwt,
        cartId = cartId,
        cartLineRequest = cartLineRequest
    )

    @DeleteMapping("/{cartId}/delete")
    fun removeCartLineByWatchIdFromCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID,
        @RequestBody @Valid cartLineRequest: CartLineRequest
    ): ResponseEntity<*> = cartService.removeCartLineByWatchIdInCartById(
        jwt = jwt,
        cartId = cartId,
        cartLineRequest = cartLineRequest
    )

    @DeleteMapping("/delete/{cartId}")
    fun removeCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID
    ): ResponseEntity<*> = cartService.removeCartById(
        jwt = jwt,
        cartId = cartId
    )

    @DeleteMapping("/delete")
    fun removeAllCartsByCustomerId(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<*> = cartService.removeAllCartsByCustomerId(jwt)
}