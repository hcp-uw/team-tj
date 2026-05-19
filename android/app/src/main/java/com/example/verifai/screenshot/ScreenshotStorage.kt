package com.example.verifai.screenshot

import android.content.Context
import java.io.File

object ScreenshotStorage {

    fun screenshotsDir(context: Context): File =
        File(context.filesDir, "screenshots").apply { mkdirs() }

    fun listScreenshots(context: Context): List<File> =
        screenshotsDir(context)
            .listFiles { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
