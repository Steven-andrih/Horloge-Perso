package com.horloge.perso

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class ClockWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ClockEngine()

    inner class ClockEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val prefs = Prefs.get(this@ClockWallpaperService)
        private val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        private var visible = false

        private val ticker = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    val now = System.currentTimeMillis()
                    handler.postDelayed(this, 60_000L - now % 60_000L + 50L)
                }
            }
        }

        // Redessine quand l'écran s'allume ou quand on déverrouille
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                drawFrame()
                handler.postDelayed({ drawFrame() }, 250)
                handler.postDelayed({ drawFrame() }, 800)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
            val f = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            this@ClockWallpaperService.registerReceiver(receiver, f)
        }

        override fun onDestroy() {
            handler.removeCallbacksAndMessages(null)
            prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
            try { this@ClockWallpaperService.unregisterReceiver(receiver) } catch (_: Exception) {}
            super.onDestroy()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(ticker)
            if (isVisible) ticker.run()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            drawFrame()
        }

        private fun drawFrame() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    val locked = if (isPreview) true else keyguard.isKeyguardLocked
                    val show = isPreview || when (prefs.mode) {
                        1 -> locked
                        2 -> !locked
                        else -> true
                    }
                    ClockRenderer.draw(canvas, canvas.width, canvas.height, prefs, show, locked)
                }
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
