package com.don11995.minbrightness

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrightnessHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    var brightness: Int
        get() = Settings.System.getInt(
            cr,
            Settings.System.SCREEN_BRIGHTNESS
        )
        set(value) {
            val brightness = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
            Settings.System.putInt(
                cr,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
        }

    private val prefs = context.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE
    )

    var minBrightness: Int = prefs.getInt(
        PREFS_MIN_BRIGHTNESS,
        DEFAULT_MIN_BRIGHTNESS
    )
        set(value) {
            if (field == value) return
            field = value
            prefs.edit {
                putInt(PREFS_MIN_BRIGHTNESS, value)
            }
        }

    private val cr = context.contentResolver
    private val brightnessObserver = object : ContentObserver(
        Handler(Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean) {
            notifyBrightnessChanged()
        }
    }
    private val brightnessListeners = mutableListOf<BrightnessListener>()

    init {
        val brightnessUri: Uri = Settings.System.getUriFor(
            Settings.System.SCREEN_BRIGHTNESS
        )
        cr.registerContentObserver(
            brightnessUri,
            false,
            brightnessObserver
        )
    }

    fun canWriteSettings(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }

    @TargetApi(Build.VERSION_CODES.M)
    fun goToSettingsPermSetup() {
        val packageName = context.packageName
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse("package:$packageName")
        }
        context.startActivity(intent)
    }

    fun tryInvalidateBrightness() {
        if (!canWriteSettings()) return
        if (brightness < minBrightness) {
            brightness = minBrightness
        }
    }

    fun addListener(listener: BrightnessListener) {
        synchronized(brightnessListeners) {
            brightnessListeners.add(listener)
        }
    }

    fun removeListener(listener: BrightnessListener) {
        synchronized(brightnessListeners) {
            brightnessListeners.remove(listener)
        }
    }

    internal fun notifyBrightnessChanged() {
        synchronized(brightnessListeners) {
            brightnessListeners.forEach { it.onBrightnessChanged() }
        }
    }

    interface BrightnessListener {
        fun onBrightnessChanged()
    }

    companion object {
        private const val MAX_BRIGHTNESS = 1023
        private const val MIN_BRIGHTNESS = 0
        private const val DEFAULT_MIN_BRIGHTNESS = 128
        private const val PREFS = "settings"
        private const val PREFS_MIN_BRIGHTNESS = "min_brightness"
    }
}
