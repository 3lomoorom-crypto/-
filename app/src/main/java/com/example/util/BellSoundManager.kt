package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BellSoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun playBell(ringCount: Int, onChime: (Int) -> Unit = {}) {
        scope.launch {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val originalAlarmVolume = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxAlarmVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM)

            try {
                // Ensure alarm volume is high for finding the lost device
                maxAlarmVolume?.let { max ->
                    audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
                }

                repeat(ringCount) { index ->
                    onChime(index + 1)
                    vibrate()
                    playChimeAudio()
                    delay(700)
                    if (index < ringCount - 1) {
                        delay(200)
                    }
                }
            } catch (e: Exception) {
                // Fallback tone generator
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    repeat(ringCount) { index ->
                        onChime(index + 1)
                        vibrate()
                        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                        delay(650)
                    }
                } catch (_: Exception) {}
                finally {
                    try { toneGen?.release() } catch (_: Exception) {}
                }
            } finally {
                // Restore original volume if desired or leave as is
                originalAlarmVolume?.let { orig ->
                    delay(500)
                    try { audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, orig, 0) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun playChimeAudio() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                prepare()
                start()
            }
            mediaPlayer.setOnCompletionListener { mp ->
                try { mp.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            // Fallback tone
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
            scope.launch {
                delay(500)
                try { tone.release() } catch (_: Exception) {}
            }
        }
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(500)
                }
            }
        } catch (_: Exception) {}
    }
}
