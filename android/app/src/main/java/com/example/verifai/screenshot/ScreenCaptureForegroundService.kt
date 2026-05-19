package com.example.verifai.screenshot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.concurrent.Executors
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.verifai.R
import com.example.verifai.analysis.ImageAnalysisWorkflow
import com.example.verifai.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking

class ScreenCaptureForegroundService : Service() {

    private var mediaProjection: MediaProjection? = null
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CAPTURE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = intent.getParcelableExtraCompat(EXTRA_RESULT_DATA, Intent::class.java)
        val selection = intent.getParcelableExtraCompat(EXTRA_SELECTION, Rect::class.java)

        if (resultData == null || selection == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        NotificationHelper.ensureChannel(this)
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_capturing_title))
            .setContentText(getString(R.string.notification_capturing_text))
            .setOngoing(true)
            .setSilent(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            },
        )

        val projectionManager =
            getSystemService(MediaProjectionManager::class.java)
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        stopSelf()
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }

        mainHandler.postDelayed({
            captureExecutor.execute {
                runCapture(selection)
            }
        }, CAPTURE_DELAY_MS)

        return START_NOT_STICKY
    }

    private fun runCapture(selection: Rect) {
        val projection = mediaProjection
        if (projection == null) {
            NotificationHelper.showScreenshotFailedNotification(this)
            mainHandler.post { stopSelf() }
            return
        }

        try {
            val metrics = ScreenCaptureHelper.displayMetrics(this)
            val fullBitmap = ScreenCaptureHelper.captureDisplay(projection, metrics)
            val cropped = ScreenCaptureHelper.cropBitmap(
                fullBitmap,
                selection,
                metrics.widthPixels,
                metrics.heightPixels,
            )
            if (fullBitmap != cropped) {
                fullBitmap.recycle()
            }
            val savedFile = ScreenCaptureHelper.saveBitmap(this, cropped)
            cropped.recycle()

            val analysisId = runBlocking {
                ImageAnalysisWorkflow.processImage(applicationContext, savedFile)
            }
            if (analysisId == null && FirebaseAuth.getInstance().currentUser == null) {
                NotificationHelper.showScreenshotSavedNotification(this, savedFile)
            }
        } catch (_: Exception) {
            NotificationHelper.showScreenshotFailedNotification(this)
        } finally {
            mediaProjection?.stop()
            mediaProjection = null
            mainHandler.post {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        captureExecutor.shutdownNow()
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
    }

    private fun <T> Intent.getParcelableExtraCompat(name: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(name)
        }
    }

    companion object {
        const val ACTION_CAPTURE = "com.example.verifai.action.CAPTURE_SCREEN"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_SELECTION = "extra_selection"

        private const val NOTIFICATION_ID = 1002
        private const val CAPTURE_DELAY_MS = 200L

        fun startCapture(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            selection: Rect,
        ) {
            val intent = Intent(context, ScreenCaptureForegroundService::class.java).apply {
                action = ACTION_CAPTURE
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_SELECTION, selection)
            }
            context.startForegroundService(intent)
        }
    }
}
