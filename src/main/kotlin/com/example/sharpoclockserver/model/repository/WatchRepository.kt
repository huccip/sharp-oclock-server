package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.Watch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WatchRepository : JpaRepository<Watch, UUID> {
}