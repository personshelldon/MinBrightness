package com.don11995.minbrightness

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BrightnessService : Service() {

    @Inject
    lateinit var brightnessHelper: BrightnessHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initChannelIfNeeded()
        startForegroundIfNeeded()
        invalidateBrightness(brightnessHelper.getBrightness())

        brightnessHelper.setBrightnessListener {
            invalidateBrightness(it)
        }
    }

    override fun onDestroy() {
        brightnessHelper.release()
        super.onDestroy()
    }

    private fun invalidateBrightness(current: Int) {
        val minBrightness = brightnessHelper.minBrightness
        if (current < minBrightness &&
            brightnessHelper.canWriteSettings()
        ) {
            brightnessHelper.setBrightness(minBrightness)
        }
    }

    private fun initChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun initChannel() {
        val nm = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)
    }

    private fun startForegroundIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        startForeground(NOTIFICATION_ID, createNotification())
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSmallIcon(R.drawable.ic_sun)
            .setContentText(getString(R.string.service_text))
            .setContentIntent(createNotificationIntent())
            .build()

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationIntent(): PendingIntent {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "brightness_service_id"

        fun startService(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(
                    context,
                    BrightnessService::class.java
                )
            )
        }
    }
}
