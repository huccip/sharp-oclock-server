package com.example.sharpoclockserver.model

import jakarta.persistence.*
import java.time.Instant

@Embeddable
data class RegisteredDevice(
    @Column(name = "last_signed_in_at", nullable = false)
    var lastSignedInAt: Instant = Instant.now(),

    @Column(name = "device_id", nullable = false, updatable = false)
    val deviceId: String,
    @Column(name = "device_name", nullable = false)
    val deviceName: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, updatable = false)
    val deviceType: DeviceType,
)

enum class DeviceType {
    Tablet,
    Phone,
    Desktop,
}