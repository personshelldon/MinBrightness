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
    var minBrightness: Int = -1
        get() {
            if (field < 0) {
                field = prefs.getInt(
                    PREFS_MIN_BRIGHTNESS,
                    DEFAULT_MIN_BRIGHTNESS
                )
            }
            return field
        }
        set(value) {
            if (field == value) return
            field = value
            val brightness = value.coerceIn(
                MIN_BRIGHTNESS,
                MAX_BRIGHTNESS
            )
            prefs.edit {
                putInt(PREFS_MIN_BRIGHTNESS, brightness)
            }
        }


    private val prefs = context.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE
    )
    private val cr = context.contentResolver
    private val brightnessUri: Uri = Settings.System.getUriFor(
        Settings.System.SCREEN_BRIGHTNESS
    )
    private val handler = Handler(Looper.getMainLooper())
    private val brightnessObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            brightnessListener?.invoke(getBrightness())
        }
    }

    internal var brightnessListener: ((value: Int) -> Unit)? = null

    init {
        cr.registerContentObserver(
            brightnessUri,
            false,
            brightnessObserver
        )
        minBrightness = prefs.getInt(
            PREFS_MIN_BRIGHTNESS,
            DEFAULT_MIN_BRIGHTNESS
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
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        context.startActivity(intent)
    }

    fun setBrightness(value: Int) {
        val brightness = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        Settings.System.putInt(
            cr,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness
        )
    }

    fun getBrightness(): Int =
        Settings.System.getInt(
            cr,
            Settings.System.SCREEN_BRIGHTNESS
        )

    fun setBrightnessListener(l: (value: Int) -> Unit) {
        brightnessListener = l
    }

    fun release() {
        cr.unregisterContentObserver(brightnessObserver)
    }

    companion object {
        private const val MAX_BRIGHTNESS = 1023
        private const val MIN_BRIGHTNESS = 0
        private const val DEFAULT_MIN_BRIGHTNESS = 128
        private const val PREFS = "settings"
        private const val PREFS_MIN_BRIGHTNESS = "min_brightness"
    }
}
