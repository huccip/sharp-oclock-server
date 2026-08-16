package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CustomerRepository: JpaRepository<Customer, UUID> {
    fun findByEmailOrPseudo(email: String, pseudo: String): Customer?
}