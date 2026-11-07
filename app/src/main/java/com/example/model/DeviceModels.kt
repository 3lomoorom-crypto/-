package com.example.model

data class Device(
    val id: String,
    val name: String,
    val model: String,
    val isCurrentDevice: Boolean,
    val isPaired: Boolean,
    val isOnline: Boolean = true,
    val battery: Int = 88,
    val latitude: Double = 13.7563,
    val longitude: Double = 100.5018,
    val locationName: String = "เขตพระนคร กรุงเทพมหานคร",
    val lastSeen: String = "ออนไลน์ขณะนี้",
    val isLocked: Boolean = false,
    val lockMessage: String = "โทรศัพท์เครื่องนี้สูญหาย โปรดติดต่อเจ้าของเครื่องเพื่อส่งคืน",
    val contactPhone: String = "089-123-4567",
    val unlockPin: String = "1234"
)

data class PairRequest(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val targetId: String,
    val targetName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BellAlert(
    val id: String,
    val fromDeviceName: String,
    val targetDeviceName: String,
    val ringCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
