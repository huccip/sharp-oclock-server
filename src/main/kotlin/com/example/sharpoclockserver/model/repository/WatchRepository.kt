package com.example.sharpoclockserver.model.repository

import com.example.sharpoclockserver.model.Watch
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WatchRepository : JpaRepository<Watch, UUID> {

    fun findAllByOrderByCreatedAtDesc(
        pageable: Pageable,
    ): Page<Watch>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select watch from Watch watch where watch.id in :watchIds order by watch.id")
    fun findAllByIdForUpdate(
        @Param("watchIds") watchIds: Collection<UUID>,
    ): List<Watch>
}
