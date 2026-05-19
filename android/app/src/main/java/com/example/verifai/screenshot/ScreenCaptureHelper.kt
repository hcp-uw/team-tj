package com.example.verifai.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object ScreenCaptureHelper {

    fun captureDisplay(
        projection: MediaProjection,
        metrics: DisplayMetrics,
    ): Bitmap {
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null
        try {
            virtualDisplay = projection.createVirtualDisplay(
                "verifai-capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null,
            )

            repeat(15) {
                Thread.sleep(50)
                reader.acquireLatestImage()?.use { image ->
                    return imageToBitmap(image)
                }
            }
            error("Timed out waiting for screen capture")
        } finally {
            virtualDisplay?.release()
            reader.close()
        }
    }

    fun cropBitmap(bitmap: Bitmap, selection: Rect, screenWidth: Int, screenHeight: Int): Bitmap {
        val scaleX = bitmap.width.toFloat() / screenWidth
        val scaleY = bitmap.height.toFloat() / screenHeight

        val left = (selection.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val top = (selection.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val right = (selection.right * scaleX).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (selection.bottom * scaleY).toInt().coerceIn(top + 1, bitmap.height)

        val cropWidth = right - left
        val cropHeight = bottom - top
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): File {
        val file = File(ScreenshotStorage.screenshotsDir(context), "capture_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file
    }

    fun displayMetrics(context: Context): DisplayMetrics {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    fun normalizeSelection(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        screenWidth: Int,
        screenHeight: Int,
        minSizePx: Int = 48,
    ): Rect? {
        val left = min(startX, endX).toInt().coerceIn(0, screenWidth)
        val top = min(startY, endY).toInt().coerceIn(0, screenHeight)
        val right = max(startX, endX).toInt().coerceIn(0, screenWidth)
        val bottom = max(startY, endY).toInt().coerceIn(0, screenHeight)
        if (right - left < minSizePx || bottom - top < minSizePx) return null
        return Rect(left, top, right, bottom)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        }
    }
}
