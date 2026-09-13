package com.gesturedot.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import com.gesturedot.app.gesture.GestureAction
import com.gesturedot.app.gesture.GestureStep

class GesturePlayer(
    private val service: AccessibilityService,
) {
    fun play(action: GestureAction, onFinished: (Boolean) -> Unit = {}) {
        val step = action.steps.firstOrNull()
        if (step == null) {
            onFinished(false)
            return
        }
        playStep(step, onFinished)
    }

    private fun playStep(step: GestureStep, onFinished: (Boolean) -> Unit) {
        val bounds = displayBounds()
        val path = Path()
        val duration = when (step) {
            is GestureStep.Swipe -> {
                step.points.forEachIndexed { index, point ->
                    val x = bounds.left + point.x * bounds.width()
                    val y = bounds.top + point.y * bounds.height()
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                step.durationMs
            }

            is GestureStep.Tap -> {
                val x = bounds.left + step.point.x * bounds.width()
                val y = bounds.top + step.point.y * bounds.height()
                path.moveTo(x, y)
                durationForTap(step)
            }
        }

        val description = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        val accepted = service.dispatchGesture(
            description,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = onFinished(true)
                override fun onCancelled(gestureDescription: GestureDescription?) = onFinished(false)
            },
            null,
        )
        if (!accepted) onFinished(false)
    }

    private fun durationForTap(step: GestureStep.Tap) = step.holdMs.coerceIn(1, 60_000)

    @Suppress("DEPRECATION")
    private fun displayBounds(): Rect {
        val windowManager = service.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            Rect(0, 0, service.resources.displayMetrics.widthPixels, service.resources.displayMetrics.heightPixels)
        }
    }
}
