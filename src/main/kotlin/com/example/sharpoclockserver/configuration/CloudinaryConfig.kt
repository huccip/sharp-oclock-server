package com.example.sharpoclockserver.configuration

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CloudinaryConfig {

    @Bean
    fun cloudinary(
        @Value($$"${app.cloudinary.url}")
        cloudinaryUrl: String,
    ): Cloudinary = Cloudinary(cloudinaryUrl)
}