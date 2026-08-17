package com.example.sharpoclockserver.auth

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin")
class AdminAuthController {

    @GetMapping("/sign-in")
    fun signIn() = "auth/admin/sign-in"

    @GetMapping("/dashboard")
    fun dashboard() = "auth/admin/dashboard"
}