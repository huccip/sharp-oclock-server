package com.example.sharpoclockserver.checkout.cart

import com.example.sharpoclockserver.model.Cart
import com.example.sharpoclockserver.model.CartLine
import com.example.sharpoclockserver.model.repository.CartRepository
import com.example.sharpoclockserver.model.repository.CustomerRepository
import com.example.sharpoclockserver.model.repository.WatchRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class CartService(
    val cartRepository: CartRepository,
    val watchRepository: WatchRepository,
    val customerRepository: CustomerRepository,
) {

    @Transactional
    fun createCart(
        jwt: Jwt,
        createCartRequest: CartController.CreateCartRequest,
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val customer = customerRepository.findById(customerId).orElse(null)
            ?: return notFound(NotFound.CUSTOMER)
        val label = createCartRequest.label?.trim()?.takeIf { it.isNotEmpty() }

        val cart = cartRepository.save(
            Cart(
                id = UUID.randomUUID(),
                customer = customer,
                label = label,
            )
        )

        return ResponseEntity(
            CartController.CreateCartResponse(
                cartId = cart.id,
                label = cart.label,
            ),
            HttpStatus.CREATED,
        )
    }

    @Transactional
    fun renameCartById(
        jwt: Jwt,
        cartId: UUID,
        renameCartRequest: CartController.RenameCartRequest,
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        cart.label = renameCartRequest.label.trim()
        cart.updatedAt = Instant.now()
        cartRepository.save(cart)

        return ResponseEntity(
            "Cart renamed to ${cart.label}",
            HttpStatus.OK,
        )
    }

    @Transactional(readOnly = true)
    fun getAllCartsByCustomerId(
        jwt: Jwt
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)

        return ResponseEntity(
            cartRepository.findAllByCustomerId(customerId).map { cart -> cart.toCartResponse() },
            HttpStatus.OK
        )
    }

    @Transactional(readOnly = true)
    fun getCartById(
        jwt: Jwt,
        cartId: UUID
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        return ResponseEntity(
            cart.toCartResponse(),
            HttpStatus.OK
        )
    }

    @Transactional
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

        if (watch.isOOS) return ResponseEntity(
            "This product is Out Of Stock",
            HttpStatus.NOT_ACCEPTABLE
        )

        if (cartLineRequest.watchSize !in watch.sizes) return ResponseEntity(
            "The requested watch size is not available",
            HttpStatus.BAD_REQUEST,
        )

        val existingCartLine = cart.cartLines.firstOrNull { cartLine ->
            cartLine.watch.id == watch.id && cartLine.size == cartLineRequest.watchSize
        }

        if (existingCartLine == null) {
            cart.cartLines.add(
                CartLine(
                    watch = watch,
                    size = cartLineRequest.watchSize,
                    quantity = cartLineRequest.quantity,
                )
            )
        } else {
            existingCartLine.quantity = cartLineRequest.quantity
        }
        cart.updatedAt = Instant.now()
        cartRepository.save(cart)

        return ResponseEntity(
            "Cart ${if (cart.label.isNullOrBlank()) "" else cart.label + " "}is updated!",
            HttpStatus.OK
        )
    }

    @Transactional
    fun removeCartLineFromCartById(
        jwt: Jwt,
        cartId: UUID,
        watchId: UUID,
        watchSize: Int,
    ): ResponseEntity<*> {
        if (jwt.subject.isNullOrBlank()) return unauthorized()

        val customerId = UUID.fromString(jwt.subject!!)
        val cart = cartRepository.findById(cartId).orElse(null) ?: return notFound()

        if (cart.customer.id != customerId) return unauthorized()

        val removed = cart.cartLines.removeIf { cartLine ->
            cartLine.watch.id == watchId && cartLine.size == watchSize
        }
        if (!removed) return notFound(NotFound.CART_LINE)

        cart.updatedAt = Instant.now()
        cartRepository.save(cart)

        return ResponseEntity(
            "Cart updated successfully",
            HttpStatus.OK
        )
    }

    @Transactional
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

    @Transactional
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

    private fun Cart.toCartResponse() = CartController.CartResponse(
        id = id,
        label = label,
        cartLines = cartLines.map { cartLine ->
            val watch = cartLine.watch

            CartController.CartLineResponse(
                watchId = watch.id,
                thumbnailUrl = watch.thumbnailUrl,
                brand = watch.brand,
                name = watch.name,
                size = cartLine.size,
                quantity = cartLine.quantity,
                price = watch.price,
                isOOS = watch.isOOS,
                isDiscounted = watch.isDiscounted,
                discountPercentage = watch.discountPercentage,
            )
        },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

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
        CART, CART_LINE, CUSTOMER, WATCH
    }
}