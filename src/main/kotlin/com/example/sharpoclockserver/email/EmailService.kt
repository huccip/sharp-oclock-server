package com.example.sharpoclockserver.email

import com.example.sharpoclockserver.model.Customer
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,

    @Value($$"${app.mail.from}")
    private val mailFrom: String
) {
    private val dateTimeFormatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy 'at' hh:mm a")
        .withZone(ZoneId.systemDefault())

    fun sendWelcomeEmail(
        customer: Customer,
    ) {
        sendEmail(
            recipient = customer.email,
            subject = "Welcome to Sharp O\'Clock",
            template = "email/welcome",
            infoValueMap = mapOf(
                "fullName" to customer.fullName,
                "pseudo" to customer.pseudo,
                "email" to customer.email,
                "address" to (customer.addressLine ?: "Not provided"),
                "age" to (customer.age?.toString() ?: "Not provided"),
                "devices" to customer.lastSignedInDeviceList.map {
                    val formattedLastSignedInAt = dateTimeFormatter.format(it.lastSignedInAt)

                    return@map mapOf(
                        "name" to it.deviceName,
                        "type" to it.deviceType,
                        "signedInAt" to formattedLastSignedInAt
                    )
                },
            ),
        )
    }

    fun sendPasswordResetRequestEmail(
        customer: Customer,
        otp: String,
        expiryMinutes: Long,
    ) {
        sendEmail(
            recipient = customer.email,
            subject = "Sharp O\'Clock Password Reset",
            template = "email/password-reset-request",
            infoValueMap = mapOf(
                "fullName" to customer.fullName,
                "otp" to otp,
                "expiryMinutes" to expiryMinutes,
            ),
        )
    }

    fun sendPasswordResetConfirmEmail(
        customer: Customer,
    ) {
        sendEmail(
            recipient = customer.email,
            subject = "Your password was changed!",
            template = "email/password-reset-confirm",
            infoValueMap = mapOf(
                "fullName" to customer.fullName,
            )
        )
    }

    private fun sendEmail(
        recipient: String,
        subject: String,
        template: String,
        infoValueMap: Map<String, Any?>
    ) {
        val context = Context()

        infoValueMap.forEach { (info, value) ->
            context.setVariable(info, value)
        }

        val html = templateEngine.process(template, context)

        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, StandardCharsets.UTF_8.name())

        helper.setFrom(mailFrom)
        helper.setTo(recipient)
        helper.setSubject(subject)
        helper.setText(html, true)

        mailSender.send(message)
    }
}