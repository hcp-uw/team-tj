package com.example.verifai.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.verifai.R

object NotificationHelper {

    const val CHANNEL_ID = "quick_settings"
    private const val NOTIFICATION_ID_CLICK = 1001
    private const val NOTIFICATION_ID_SAVED = 1003
    private const val NOTIFICATION_ID_FAILED = 1004
    private const val NOTIFICATION_ID_ANALYZING = 1006
    private const val NOTIFICATION_ID_ANALYSIS_DONE = 1007
    private const val NOTIFICATION_ID_ANALYSIS_FAILED = 1008
    private const val NOTIFICATION_ID_SIGN_IN = 1009

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun showQuickSettingsClickNotification(context: Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_clicked_title))
            .setContentText(context.getString(R.string.notification_clicked_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (!canPostNotifications(context)) return

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CLICK, notification)
    }

    fun showScreenshotSavedNotification(context: Context, file: java.io.File) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_saved_title))
            .setContentText(context.getString(R.string.notification_saved_text, file.name))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(file.absolutePath),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (!canPostNotifications(context)) return

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SAVED, notification)
    }

    fun showAnalyzingNotification(context: Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_analyzing_title))
            .setContentText(context.getString(R.string.notification_analyzing_text))
            .setOngoing(true)
            .setSilent(true)
            .build()
        if (!canPostNotifications(context)) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ANALYZING, notification)
    }

    fun showAnalysisCompleteNotification(context: Context, verdict: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_analysis_done_title))
            .setContentText(context.getString(R.string.notification_analysis_done_text, verdict))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (!canPostNotifications(context)) return
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ANALYZING)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ANALYSIS_DONE, notification)
    }

    fun showAnalysisFailedNotification(context: Context, message: String?) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_analysis_failed_title))
            .setContentText(message ?: context.getString(R.string.notification_analysis_failed_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (!canPostNotifications(context)) return
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ANALYZING)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ANALYSIS_FAILED, notification)
    }

    fun showSignInRequiredNotification(context: Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_sign_in_title))
            .setContentText(context.getString(R.string.notification_sign_in_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (!canPostNotifications(context)) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SIGN_IN, notification)
    }

    fun showScreenshotFailedNotification(context: Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_failed_title))
            .setContentText(context.getString(R.string.notification_failed_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (!canPostNotifications(context)) return

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FAILED, notification)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
