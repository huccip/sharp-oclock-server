package com.example.sharpoclockserver.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class RegisteredDevice(
    @Column(name = "device_id")
    val deviceId: String,
    @Column(name = "device_name")
    val deviceName: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    val deviceType: DeviceType,
)

enum class DeviceType {
    Tablet,
    Phone,
    Desktop,
}