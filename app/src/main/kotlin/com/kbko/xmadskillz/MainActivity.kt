package com.kbko.xmadskillz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbko.xmadskillz.services.FloatingBubbleService
import com.kbko.xmadskillz.services.VoiceActivationService

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 276

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_activate_omni).setOnClickListener {
            verifyAndLaunchPipeline()
        }
    }

    private fun verifyAndLaunchPipeline() {
        // Verify Draw-Over-Apps status
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, R.string.overlay_permission_msg, Toast.LENGTH_LONG).show()
            return
        }

        // Verify hardware capture rights
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            fireXMadskillzCore()
        }
    }

    private fun fireXMadskillzCore() {
        // Launch Voice Engine
        val voiceIntent = Intent(this, VoiceActivationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(voiceIntent)
        } else {
            startService(voiceIntent)
        }

        // Launch Visual Overlay System
        startService(Intent(this, FloatingBubbleService::class.java))

        Toast.makeText(this, R.string.engine_online_msg, Toast.LENGTH_SHORT).show()
        finish() // Seamless background transition
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                fireXMadskillzCore()
            } else {
                Toast.makeText(this, R.string.permissions_required_msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
