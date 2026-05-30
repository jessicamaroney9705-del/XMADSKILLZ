package com.kbko.xmadskillz.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createFloatingBubble()
        return START_STICKY
    }

    private fun createFloatingBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = FrameLayout(this)
        floatingView?.setBackgroundColor(android.graphics.Color.parseColor("#6200EE"))

        val params = WindowManager.LayoutParams(
            100, 100,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager?.addView(floatingView, params)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
        super.onDestroy()
    }
}
