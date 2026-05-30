package com.kbko.xmadskillz.services

import android.app.Service
import android.content.Intent
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kbko.xmadskillz.R

class VoiceActivationService : Service() {

    private var audioRecord: AudioRecord? = null
    private var isListening = false

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
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(1, notification, android.app.ServiceForegroundType.MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startVoiceListening() {
        isListening = true
        // Voice activation implementation
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        super.onDestroy()
    }
}