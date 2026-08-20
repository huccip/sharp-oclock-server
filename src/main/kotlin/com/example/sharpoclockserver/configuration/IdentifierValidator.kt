package com.example.sharpoclockserver.configuration

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Constraint(
    validatedBy = [IdentifierValidator::class]
)
annotation class Identifier()

class IdentifierValidator : ConstraintValidator<Identifier, String> {
    private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private val pseudoRegex = Regex("^(?=.{5,30}$)[a-zA-Z0-9]+(?:[._-][a-zA-Z0-9]+)*$")

    override fun isValid(value: String, context: ConstraintValidatorContext?): Boolean {
        return emailRegex.matches(value) || pseudoRegex.matches(value)
    }
}