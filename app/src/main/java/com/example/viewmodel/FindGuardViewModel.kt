package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BellAlert
import com.example.model.Device
import com.example.model.PairRequest
import com.example.network.RealtimeSyncService
import com.example.util.BellSoundManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class UiState(
    val currentTab: Int = 0,
    val viewingDeviceId: String = "DEV-A81",
    val realDeviceId: String = "",
    val realDeviceName: String = "",
    val activePairCode: String = "FG8492",
    val isOnlineSynced: Boolean = false,
    val deviceA: Device = Device(
        id = "DEV-A81",
        name = "Pixel 8 Pro (เครื่อง A)",
        model = "Google Pixel 8 Pro",
        isCurrentDevice = true,
        isPaired = false,
        battery = 89,
        latitude = 13.7563,
        longitude = 100.5018,
        locationName = "พระนคร กรุงเทพมหานคร",
        lastSeen = "ออนไลน์",
        isLocked = false,
        lockMessage = "เครื่องสูญหาย โปรดติดต่อส่งคืน",
        contactPhone = "081-234-5678",
        unlockPin = "1234"
    ),
    val deviceB: Device = Device(
        id = "DEV-B92",
        name = "Galaxy S24 (เครื่อง B)",
        model = "Samsung Galaxy S24 Ultra",
        isCurrentDevice = false,
        isPaired = false,
        battery = 76,
        latitude = 13.7469,
        longitude = 100.5349,
        locationName = "ปทุมวัน กรุงเทพมหานคร",
        lastSeen = "ออนไลน์",
        isLocked = false,
        lockMessage = "เครื่องสูญหาย โปรดติดต่อกลับด่วน!",
        contactPhone = "089-987-6543",
        unlockPin = "1234"
    ),
    val pairCodeInput: String = "",
    val activePairRequest: PairRequest? = null,
    val deviceAAccepted: Boolean? = null,
    val deviceBAccepted: Boolean? = null,
    val isScanningQr: Boolean = false,
    val showQrDialog: Boolean = false,
    val isRingingNow: Boolean = false,
    val currentRingingCount: Int = 0,
    val bellHistory: List<BellAlert> = emptyList(),
    val pinInput: String = "",
    val pinError: Boolean = false,
    val toastMessage: String? = null
)

class FindGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("findguard_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val soundManager = BellSoundManager(application.applicationContext)
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val syncService = RealtimeSyncService { incoming ->
        handleIncomingSyncMessage(incoming)
    }

    private var locationCallback: LocationCallback? = null

    init {
        // Initialize persistent Real Device identity
        val savedId = prefs.getString("device_id", null) ?: run {
            val newId = "DEV-" + (1000..9999).random()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }

        val deviceModelName = Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + Build.MODEL
        val batteryPct = getRealBatteryLevel()
        val savedPairCode = prefs.getString("pair_code", "FG8492") ?: "FG8492"
        val savedPaired = prefs.getBoolean("is_paired", false)

        _uiState.update { state ->
            state.copy(
                realDeviceId = savedId,
                realDeviceName = deviceModelName,
                activePairCode = savedPairCode,
                deviceA = state.deviceA.copy(
                    id = savedId,
                    name = "$deviceModelName (เครื่องนี้)",
                    battery = batteryPct,
                    isPaired = savedPaired
                ),
                deviceB = state.deviceB.copy(
                    isPaired = savedPaired
                ),
                viewingDeviceId = savedId
            )
        }

        // Connect real cloud sync channel
        syncService.startListening(savedPairCode, savedId)
        _uiState.update { it.copy(isOnlineSynced = true) }

        // Start listening to real phone GPS updates
        startRealGpsUpdates()
    }

    private fun getRealBatteryLevel(): Int {
        val app = getApplication<Application>()
        val bm = app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        return if (level in 1..100) level else 85
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(currentTab = tabIndex) }
    }

    fun switchViewingDevice(deviceId: String) {
        _uiState.update { it.copy(viewingDeviceId = deviceId, pinInput = "", pinError = false) }
    }

    fun getCurrentDevice(): Device {
        val state = _uiState.value
        return if (state.viewingDeviceId == state.deviceA.id) state.deviceA else state.deviceB
    }

    fun getRemoteDevice(): Device {
        val state = _uiState.value
        return if (state.viewingDeviceId == state.deviceA.id) state.deviceB else state.deviceA
    }

    fun updatePairCodeInput(code: String) {
        _uiState.update { it.copy(pairCodeInput = code.uppercase()) }
    }

    fun setShowQrDialog(show: Boolean) {
        _uiState.update { it.copy(showQrDialog = show) }
    }

    fun changeActivePairCode(newCode: String) {
        val clean = newCode.replace("-", "").uppercase()
        if (clean.length >= 4) {
            prefs.edit().putString("pair_code", clean).apply()
            _uiState.update { it.copy(activePairCode = clean) }
            syncService.startListening(clean, _uiState.value.realDeviceId)
        }
    }

    // Initiate pairing flow between the 2 devices
    fun initiatePairing(targetCode: String = "") {
        val state = _uiState.value
        val pairCode = if (targetCode.isNotBlank()) targetCode else state.activePairCode
        changeActivePairCode(pairCode)

        val fromDev = getCurrentDevice()
        val targetDev = getRemoteDevice()

        val req = PairRequest(
            id = "REQ-${System.currentTimeMillis() % 10000}",
            requesterId = fromDev.id,
            requesterName = fromDev.name,
            targetId = targetDev.id,
            targetName = targetDev.name
        )

        _uiState.update {
            it.copy(
                activePairRequest = req,
                deviceAAccepted = null,
                deviceBAccepted = null,
                showQrDialog = false,
                pairCodeInput = ""
            )
        }

        // Broadcast to real cloud
        syncService.broadcastMessage(
            pairCode = pairCode,
            type = "PAIR_REQUEST",
            senderId = fromDev.id,
            senderName = fromDev.name
        )
    }

    fun respondPairingDeviceA(accepted: Boolean) {
        _uiState.update { it.copy(deviceAAccepted = accepted) }
        checkPairingResolution()

        val currentDev = getCurrentDevice()
        syncService.broadcastMessage(
            pairCode = _uiState.value.activePairCode,
            type = if (accepted) "PAIR_ACCEPT" else "PAIR_DECLINE",
            senderId = currentDev.id,
            senderName = currentDev.name
        )
    }

    fun respondPairingDeviceB(accepted: Boolean) {
        _uiState.update { it.copy(deviceBAccepted = accepted) }
        checkPairingResolution()

        val remoteDev = getRemoteDevice()
        syncService.broadcastMessage(
            pairCode = _uiState.value.activePairCode,
            type = if (accepted) "PAIR_ACCEPT" else "PAIR_DECLINE",
            senderId = remoteDev.id,
            senderName = remoteDev.name
        )
    }

    private fun checkPairingResolution() {
        val state = _uiState.value
        val aAns = state.deviceAAccepted
        val bAns = state.deviceBAccepted

        if (aAns != null && bAns != null) {
            if (aAns && bAns) {
                prefs.edit().putBoolean("is_paired", true).apply()
                _uiState.update {
                    it.copy(
                        activePairRequest = null,
                        deviceA = it.deviceA.copy(isPaired = true),
                        deviceB = it.deviceB.copy(isPaired = true),
                        toastMessage = "เชื่อมต่ออุปกรณ์ทั้งสองสำเร็จแล้ว!"
                    )
                }
                // Broadcast current GPS location to the new partner
                broadcastCurrentLocation()
            } else {
                _uiState.update {
                    it.copy(
                        activePairRequest = null,
                        toastMessage = "การเชื่อมต่อถูกยกเลิก (มีอุปกรณ์กดยกเลิก)"
                    )
                }
            }
        }
    }

    fun unpairDevices() {
        prefs.edit().putBoolean("is_paired", false).apply()
        _uiState.update {
            it.copy(
                deviceA = it.deviceA.copy(isPaired = false),
                deviceB = it.deviceB.copy(isPaired = false),
                toastMessage = "ยกเลิกการเชื่อมต่ออุปกรณ์แล้ว"
            )
        }
        val currentDev = getCurrentDevice()
        syncService.broadcastMessage(
            pairCode = _uiState.value.activePairCode,
            type = "UNPAIR",
            senderId = currentDev.id,
            senderName = currentDev.name
        )
    }

    // Trigger Bell chime (1 time or 2 times)
    fun triggerBell(ringCount: Int) {
        val fromDev = getCurrentDevice()
        val targetDev = getRemoteDevice()

        val alert = BellAlert(
            id = "BELL-${System.currentTimeMillis() % 10000}",
            fromDeviceName = fromDev.name,
            targetDeviceName = targetDev.name,
            ringCount = ringCount
        )

        _uiState.update {
            it.copy(
                isRingingNow = true,
                currentRingingCount = ringCount,
                bellHistory = listOf(alert) + it.bellHistory.take(15),
                toastMessage = "ส่งสัญญาณเสียง $ringCount ครั้ง ไปยัง ${targetDev.name}"
            )
        }

        // Play audio and vibration on this device
        soundManager.playBell(ringCount) { currentChimeIndex ->
            _uiState.update { it.copy(currentRingingCount = currentChimeIndex) }
        }

        // Broadcast over cloud to target physical phone!
        val data = JSONObject().apply {
            put("ringCount", ringCount)
        }
        syncService.broadcastMessage(
            pairCode = _uiState.value.activePairCode,
            type = "RING_BELL",
            senderId = fromDev.id,
            senderName = fromDev.name,
            data = data
        )

        viewModelScope.launch {
            delay((ringCount * 800L) + 600L)
            _uiState.update { it.copy(isRingingNow = false) }
        }
    }

    // Remote Lock Target Device
    fun lockRemoteDevice(customMessage: String? = null, contactPhone: String? = null) {
        val fromDev = getCurrentDevice()
        val targetDev = getRemoteDevice()
        val updatedMessage = customMessage ?: targetDev.lockMessage
        val updatedPhone = contactPhone ?: targetDev.contactPhone

        _uiState.update { state ->
            if (state.viewingDeviceId == state.deviceA.id) {
                state.copy(
                    deviceB = state.deviceB.copy(
                        isLocked = true,
                        lockMessage = updatedMessage,
                        contactPhone = updatedPhone
                    ),
                    toastMessage = "สั่งล็อคเครื่อง ${state.deviceB.name} สำเร็จแล้ว!"
                )
            } else {
                state.copy(
                    deviceA = state.deviceA.copy(
                        isLocked = true,
                        lockMessage = updatedMessage,
                        contactPhone = updatedPhone
                    ),
                    toastMessage = "สั่งล็อคเครื่อง ${state.deviceA.name} สำเร็จแล้ว!"
                )
            }
        }

        // Broadcast Lock Command to the other physical device
        val data = JSONObject().apply {
            put("message", updatedMessage)
            put("phone", updatedPhone)
            put("pin", targetDev.unlockPin)
        }
        syncService.broadcastMessage(
            pairCode = _uiState.value.activePairCode,
            type = "LOCK_DEVICE",
            senderId = fromDev.id,
            senderName = fromDev.name,
            data = data
        )
    }

    // Remote Unlock Target Device
    fun unlockRemoteDevice() {
        val fromDev = getCurrentDevice()
        val targetDev = getRemoteDevice()

        _uiState.update { state ->
            if (state.viewingDeviceId == state.deviceA.id) {
                state.copy(
                    deviceB = state.deviceB.copy(isLocked = false),
                    toastMessage = "สั่งปลดล็อคเครื่อง ${state.deviceB.name} แล้ว"
                )
            } else {
                state.copy(
                    deviceA = state.deviceA.copy(isLocked = false),
                    toastMessage = "สั่งปลดล็อคเครื่อง ${state.deviceA.name} แล้ว"
                )
            }
        }

        syncService.broadcastMessage(
            pairCode = _uiState.value.activePairCode,
            type = "UNLOCK_DEVICE",
            senderId = fromDev.id,
            senderName = fromDev.name
        )
    }

    fun appendPinDigit(digit: String) {
        val currentPin = _uiState.value.pinInput
        if (currentPin.length < 4) {
            val newPin = currentPin + digit
            _uiState.update { it.copy(pinInput = newPin, pinError = false) }

            if (newPin.length == 4) {
                verifyPinAndUnlock(newPin)
            }
        }
    }

    fun deletePinDigit() {
        val currentPin = _uiState.value.pinInput
        if (currentPin.isNotEmpty()) {
            _uiState.update { it.copy(pinInput = currentPin.dropLast(1), pinError = false) }
        }
    }

    private fun verifyPinAndUnlock(enteredPin: String) {
        val currentDev = getCurrentDevice()
        if (enteredPin == currentDev.unlockPin) {
            _uiState.update { state ->
                val newDevA = if (currentDev.id == state.deviceA.id) state.deviceA.copy(isLocked = false) else state.deviceA
                val newDevB = if (currentDev.id == state.deviceB.id) state.deviceB.copy(isLocked = false) else state.deviceB
                state.copy(
                    deviceA = newDevA,
                    deviceB = newDevB,
                    pinInput = "",
                    pinError = false,
                    toastMessage = "ปลดล็อคอุปกรณ์ด้วยรหัสผ่านสำเร็จ!"
                )
            }
            syncService.broadcastMessage(
                pairCode = _uiState.value.activePairCode,
                type = "UNLOCK_DEVICE",
                senderId = currentDev.id,
                senderName = currentDev.name
            )
        } else {
            _uiState.update { it.copy(pinInput = "", pinError = true, toastMessage = "รหัส PIN ไม่ถูกต้อง (ค่าเริ่มต้นคือ 1234)") }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).roundToInt()
    }

    fun openInGoogleMaps(context: Context, lat: Double, lon: Double, label: String) {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealGpsUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 8000L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    updateDeviceLocation(loc)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.w("FindGuard", "GPS updates permission pending: ${e.message}")
        }
    }

    private fun updateDeviceLocation(loc: Location) {
        _uiState.update { state ->
            val updatedA = state.deviceA.copy(
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationName = "พิกัดสด: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}"
            )
            state.copy(deviceA = updatedA)
        }
        broadcastCurrentLocation()
    }

    fun broadcastCurrentLocation() {
        val state = _uiState.value
        val data = JSONObject().apply {
            put("latitude", state.deviceA.latitude)
            put("longitude", state.deviceA.longitude)
            put("locationName", state.deviceA.locationName)
            put("battery", state.deviceA.battery)
        }
        syncService.broadcastMessage(
            pairCode = state.activePairCode,
            type = "LOCATION_UPDATE",
            senderId = state.deviceA.id,
            senderName = state.deviceA.name,
            data = data
        )
    }

    private fun handleIncomingSyncMessage(msg: RealtimeSyncService.SyncMessage) {
        viewModelScope.launch {
            when (msg.type) {
                "PAIR_REQUEST" -> {
                    val req = PairRequest(
                        id = "REQ-${System.currentTimeMillis() % 1000}",
                        requesterId = msg.senderId,
                        requesterName = msg.senderName,
                        targetId = _uiState.value.deviceA.id,
                        targetName = _uiState.value.deviceA.name
                    )
                    _uiState.update {
                        it.copy(
                            activePairRequest = req,
                            deviceAAccepted = null,
                            deviceBAccepted = null,
                            toastMessage = "มีคำขอเชื่อมต่อจาก ${msg.senderName}!"
                        )
                    }
                }
                "PAIR_ACCEPT" -> {
                    _uiState.update { state ->
                        state.copy(
                            deviceBAccepted = true,
                            deviceA = state.deviceA.copy(isPaired = true),
                            deviceB = state.deviceB.copy(
                                id = msg.senderId,
                                name = msg.senderName,
                                isPaired = true
                            ),
                            activePairRequest = null,
                            toastMessage = "${msg.senderName} กดยินยอมเชื่อมต่อแล้ว!"
                        )
                    }
                    prefs.edit().putBoolean("is_paired", true).apply()
                    broadcastCurrentLocation()
                }
                "PAIR_DECLINE" -> {
                    _uiState.update {
                        it.copy(
                            activePairRequest = null,
                            toastMessage = "${msg.senderName} ปฏิเสธการเชื่อมต่อ"
                        )
                    }
                }
                "UNPAIR" -> {
                    prefs.edit().putBoolean("is_paired", false).apply()
                    _uiState.update {
                        it.copy(
                            deviceA = it.deviceA.copy(isPaired = false),
                            deviceB = it.deviceB.copy(isPaired = false),
                            toastMessage = "อุปกรณ์ถูกยกเลิกการเชื่อมต่อโดย ${msg.senderName}"
                        )
                    }
                }
                "RING_BELL" -> {
                    val count = msg.payload.optInt("ringCount", 1)
                    _uiState.update {
                        it.copy(
                            isRingingNow = true,
                            currentRingingCount = count,
                            toastMessage = "🔔 ได้รับคำสั่งส่งเสียง $count ครั้ง จาก ${msg.senderName}!"
                        )
                    }
                    soundManager.playBell(count) { idx ->
                        _uiState.update { it.copy(currentRingingCount = idx) }
                    }
                    delay((count * 800L) + 500L)
                    _uiState.update { it.copy(isRingingNow = false) }
                }
                "LOCK_DEVICE" -> {
                    val lockMsg = msg.payload.optString("message", "โทรศัพท์นี้สูญหาย โปรดติดต่อส่งคืน")
                    val phone = msg.payload.optString("phone", "081-234-5678")
                    _uiState.update { state ->
                        state.copy(
                            deviceA = state.deviceA.copy(
                                isLocked = true,
                                lockMessage = lockMsg,
                                contactPhone = phone
                            ),
                            toastMessage = "⚠️ เครื่องของคุณถูกล็อคระยะไกลโดย ${msg.senderName}!"
                        )
                    }
                }
                "UNLOCK_DEVICE" -> {
                    _uiState.update { state ->
                        state.copy(
                            deviceA = state.deviceA.copy(isLocked = false),
                            deviceB = state.deviceB.copy(isLocked = false),
                            pinInput = "",
                            toastMessage = "ปลดล็อคเครื่องเรียบร้อยแล้ว!"
                        )
                    }
                }
                "LOCATION_UPDATE" -> {
                    val lat = msg.payload.optDouble("latitude", 13.7469)
                    val lng = msg.payload.optDouble("longitude", 100.5349)
                    val locName = msg.payload.optString("locationName", "อัปเดตตำแหน่งสด")
                    val bat = msg.payload.optInt("battery", 80)
                    _uiState.update { state ->
                        state.copy(
                            deviceB = state.deviceB.copy(
                                id = msg.senderId,
                                name = msg.senderName,
                                latitude = lat,
                                longitude = lng,
                                locationName = locName,
                                battery = bat,
                                lastSeen = "ออนไลน์ขณะนี้"
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        syncService.stopListening()
    }
}
