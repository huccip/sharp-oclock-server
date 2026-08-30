package com.example.sharpoclockserver.checkout.cart

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

// POST http://localhost:8080/checkout/carts/create: creates an empty cart with an optional label
// POST http://localhost:8080/checkout/carts/{cartId}/rename: renames a cart

// GET http://localhost:8080/checkout/carts: gets all carts belonging to the authenticated customer
// GET http://localhost:8080/checkout/carts/{cartId}: gets a cart by its id

// POST http://localhost:8080/checkout/carts/{cartId}/add: adds a watch and its selected size to a cart
// POST http://localhost:8080/checkout/carts/{cartId}/update: updates a cart line quantity

// DELETE http://localhost:8080/checkout/carts/{cartId}/lines/{watchId}?watchSize=40: deletes a cart line
// DELETE http://localhost:8080/checkout/carts/{cartId}: deletes a cart by its id
// DELETE http://localhost:8080/checkout/carts: deletes all carts belonging to the authenticated customer

@RestController
@RequestMapping("/checkout/carts")
class CartController(
    private val cartService: CartService
) {

    data class CreateCartRequest(
        @Size(max = 80)
        val label: String? = null,
    )

    data class CreateCartResponse(
        val cartId: UUID,
        val label: String?,
    )

    data class CartResponse(
        val id: UUID,
        val label: String?,
        val cartLines: List<CartLineResponse>,
        val createdAt: Instant,
        val updatedAt: Instant?,
    )

    data class CartLineResponse(
        val watchId: UUID,

        val thumbnailUrl: String,

        val brand: String,
        val name: String,
        val size: Int,
        val price: Int,

        val quantity: Int,

        val isOOS: Boolean,
        val isDiscounted: Boolean,

        val discountPercentage: Int?,
    )

    data class RenameCartRequest(
        @NotBlank
        @Size(max = 80)
        val label: String,
    )

    data class CartLineRequest(
        @NotNull
        val watchId: UUID,
        @NotNull
        @Min(1)
        @Max(200)
        val watchSize: Int,
        @NotNull
        @Min(1)
        @Max(10)
        val quantity: Int,
    )

    @PostMapping("/create")
    fun createCart(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody @Valid createCartRequest: CreateCartRequest,
    ): ResponseEntity<*> = cartService.createCart(
        jwt = jwt,
        createCartRequest = createCartRequest,
    )

    @PostMapping("/{cartId}/rename")
    fun renameCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID,
        @RequestBody @Valid renameCartRequest: RenameCartRequest,
    ): ResponseEntity<*> = cartService.renameCartById(
        jwt = jwt,
        cartId = cartId,
        renameCartRequest = renameCartRequest,
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

    @DeleteMapping("/{cartId}/lines/{watchId}")
    fun removeCartLineFromCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID,
        @PathVariable(value = "watchId") watchId: UUID,
        @RequestParam(value = "watchSize") watchSize: Int,
    ): ResponseEntity<*> = cartService.removeCartLineFromCartById(
        jwt = jwt,
        cartId = cartId,
        watchId = watchId,
        watchSize = watchSize,
    )

    @DeleteMapping("/{cartId}")
    fun removeCartById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable(value = "cartId") cartId: UUID
    ): ResponseEntity<*> = cartService.removeCartById(
        jwt = jwt,
        cartId = cartId
    )

    @DeleteMapping
    fun removeAllCartsByCustomerId(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<*> = cartService.removeAllCartsByCustomerId(jwt)
}