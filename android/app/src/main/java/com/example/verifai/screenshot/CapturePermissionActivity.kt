package com.example.verifai.screenshot

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Invisible activity used only for system permission dialogs (overlay + screen capture).
 * Finishes immediately after launching the overlay service so the user stays in their app.
 */
class CapturePermissionActivity : ComponentActivity() {

    private var waitingForOverlayPermission = false

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            finishWithoutAnimation()
            return@registerForActivityResult
        }
        RegionSelectionOverlayService.start(
            context = this,
            resultCode = result.resultCode,
            resultData = result.data!!,
        )
        finishWithoutAnimation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            OverlayPermissionHelper.requestOverlayPermission(this)
            return
        }

        if (savedInstanceState == null) {
            requestScreenCapturePermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!waitingForOverlayPermission) return

        if (OverlayPermissionHelper.canDrawOverlays(this)) {
            waitingForOverlayPermission = false
            requestScreenCapturePermission()
        } else {
            finishWithoutAnimation()
        }
    }

    private fun requestScreenCapturePermission() {
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, CapturePermissionActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
            }
    }
}
