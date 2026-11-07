package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RealtimeSyncService(
    private val onMessageReceived: (SyncMessage) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for SSE stream
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private val postClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var listeningJob: Job? = null
    private var currentChannel: String? = null

    data class SyncMessage(
        val type: String,
        val senderId: String,
        val senderName: String,
        val payload: JSONObject
    )

    fun startListening(pairCode: String, myDeviceId: String) {
        val cleanCode = pairCode.replace("-", "").uppercase()
        if (cleanCode.isBlank()) return
        val channel = "fg_ch_$cleanCode"

        if (currentChannel == channel && listeningJob?.isActive == true) {
            return
        }

        stopListening()
        currentChannel = channel

        listeningJob = scope.launch {
            while (isActive) {
                try {
                    val url = "https://ntfy.sh/$channel/json"
                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "text/event-stream")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            delay(3000)
                            return@use
                        }

                        val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                        var line: String? = null
                        while (isActive && reader.readLine().also { line = it } != null) {
                            val lineContent = line?.trim() ?: continue
                            if (lineContent.startsWith("{") && lineContent.endsWith("}")) {
                                try {
                                    val json = JSONObject(lineContent)
                                    val messageRaw = json.optString("message", "")
                                    if (messageRaw.isNotEmpty() && messageRaw.startsWith("{")) {
                                        val payloadJson = JSONObject(messageRaw)
                                        val senderId = payloadJson.optString("senderId", "")
                                        // Ignore messages sent by ourselves
                                        if (senderId != myDeviceId) {
                                            val type = payloadJson.optString("type", "")
                                            val senderName = payloadJson.optString("senderName", "")
                                            val dataObj = payloadJson.optJSONObject("data") ?: JSONObject()
                                            onMessageReceived(
                                                SyncMessage(
                                                    type = type,
                                                    senderId = senderId,
                                                    senderName = senderName,
                                                    payload = dataObj
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("RealtimeSync", "Error parsing message", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("RealtimeSync", "Stream disconnected, retrying in 4s: ${e.message}")
                    delay(4000)
                }
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        currentChannel = null
    }

    fun broadcastMessage(
        pairCode: String,
        type: String,
        senderId: String,
        senderName: String,
        data: JSONObject = JSONObject()
    ) {
        val cleanCode = pairCode.replace("-", "").uppercase()
        if (cleanCode.isBlank()) return
        val channel = "fg_ch_$cleanCode"

        scope.launch {
            try {
                val envelope = JSONObject().apply {
                    put("type", type)
                    put("senderId", senderId)
                    put("senderName", senderName)
                    put("timestamp", System.currentTimeMillis())
                    put("data", data)
                }

                val body = envelope.toString().toRequestBody("text/plain".toMediaType())
                val request = Request.Builder()
                    .url("https://ntfy.sh/$channel")
                    .post(body)
                    .build()

                postClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.e("RealtimeSync", "Failed to broadcast message", e)
            }
        }
    }
}
