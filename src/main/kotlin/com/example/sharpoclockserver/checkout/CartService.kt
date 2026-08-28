package com.example.sharpoclockserver.checkout

import com.example.sharpoclockserver.model.CartLine
import com.example.sharpoclockserver.model.repository.CartRepository
import com.example.sharpoclockserver.model.repository.WatchRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import java.util.*

@Service
class CartService(
    val cartRepository: CartRepository,
    val watchRepository: WatchRepository
) {

    fun getAllCartsByCustomerId(
        jwt: Jwt
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)

        return ResponseEntity(
            cartRepository.findAllByCustomerId(customerId),
            HttpStatus.OK
        )
    }

    fun getCartById(
        jwt: Jwt,
        cartId: UUID
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        return ResponseEntity(
            cart,
            HttpStatus.OK
        )
    }

    fun updateCartLineInCartById(
        jwt: Jwt,
        cartId: UUID,
        cartLineRequest: CartController.CartLineRequest
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        val watch = watchRepository.findById(cartLineRequest.watchId).orElse(null) ?: return notFound(NotFound.WATCH)

        val updatedCartLine = CartLine(
            watch = watch,
            size = cartLineRequest.watchSize,
            quantity = cartLineRequest.quantity
        )

        cart.cartLines.add(updatedCartLine)
        cartRepository.save(cart)

        return ResponseEntity(
            "Cart ${if (cart.label.isNullOrBlank()) "" else cart.label + " "}is updated!",
            HttpStatus.OK
        )
    }

    fun removeCartLineByWatchIdInCartById(
        jwt: Jwt,
        cartId: UUID,
        cartLineRequest: CartController.CartLineRequest
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        val watch = watchRepository.findById(cartLineRequest.watchId).orElse(null) ?: return notFound(NotFound.WATCH)

        val cartLineToRemove = CartLine(
            watch = watch,
            size = cartLineRequest.watchSize,
            quantity = cartLineRequest.quantity
        )

        cart.cartLines.remove(cartLineToRemove)
        cartRepository.save(cart)

        return ResponseEntity(
            "Cart updated successfully",
            HttpStatus.OK
        )
    }

    fun removeCartById(
        jwt: Jwt,
        cartId: UUID
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)

        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        cartRepository.delete(cart)

        return ResponseEntity(
            "Cart ${if (cart.label.isNullOrBlank()) "" else cart.label + " "}is deleted",
            HttpStatus.OK
        )
    }

    fun removeAllCartsByCustomerId(
        jwt: Jwt
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)

        cartRepository.deleteAllByCustomerId(customerId)

        return ResponseEntity(
            "Your carts have been deleted",
            HttpStatus.OK
        )
    }

    private fun unauthorized(): ResponseEntity<*> =
        ResponseEntity(
            "You are unauthorized to make this request",
            HttpStatus.UNAUTHORIZED
        )

    private fun notFound(
        what: NotFound = NotFound.CART
    ): ResponseEntity<*> =
        ResponseEntity(
            "The requested ${what.name} was not found",
            HttpStatus.NOT_FOUND
        )

    private enum class NotFound {
        CART, WATCH
    }
}