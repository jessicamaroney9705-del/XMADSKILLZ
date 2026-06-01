package com.kbko.xmadskillz.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kbko.xmadskillz.R
import kotlin.concurrent.thread

class VoiceActivationService : Service() {

    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var listenerThread: Thread? = null
    private val TAG = "VoiceActivationService"
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        startVoiceListening()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "voice_channel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("X MADSKILLZ")
            .setContentText("Voice Engine Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(1, notification, android.app.ServiceForegroundType.MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startVoiceListening() {
        if (isListening) return

        isListening = true
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        try {
            audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            audioRecord?.startRecording()
            Log.d(TAG, "Voice listening started")

            // Start listening in background thread
            listenerThread = thread(isDaemon = true) {
                val audioBuffer = ByteArray(bufferSize)
                while (isListening) {
                    val bytesRead = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (bytesRead > 0) {
                        // Process audio data here if needed
                        detectVoiceActivation(audioBuffer, bytesRead)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice listening: ${e.message}")
            isListening = false
        }
    }

    private fun detectVoiceActivation(audioBuffer: ByteArray, size: Int) {
        // Calculate RMS (Root Mean Square) to detect voice activity
        var sum = 0L
        for (i in 0 until size step 2) {
            if (i + 1 < size) {
                val sample = (audioBuffer[i].toInt() or (audioBuffer[i + 1].toInt() shl 8)).toShort()
                sum += (sample * sample).toLong()
            }
        }
        val rms = Math.sqrt(sum.toDouble() / (size / 2))
        
        // Log voice activity (threshold can be adjusted)
        if (rms > 1000) {
            Log.d(TAG, "Voice detected: RMS = $rms")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "VoiceActivationService destroyed")
        isListening = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio record: ${e.message}")
        }
        audioRecord = null
        listenerThread?.interrupt()
        super.onDestroy()
    }
}