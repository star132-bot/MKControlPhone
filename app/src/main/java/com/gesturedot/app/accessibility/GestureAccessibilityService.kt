package com.gesturedot.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast
import com.gesturedot.app.data.UserPreferences
import com.gesturedot.app.gesture.BuiltInActions
import com.gesturedot.app.gesture.TapSequenceAction
import com.gesturedot.app.gesture.TapSequenceClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class GestureAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var player: GesturePlayer
    private var bubble: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var tapCount = 0

    private val resolveTaps = Runnable {
        val action = TapSequenceClassifier.classify(tapCount)
        tapCount = 0
        when (action) {
            TapSequenceAction.DEFAULT_ACTION -> playDefault()
            TapSequenceAction.OPEN_QUICK_RING -> toast("快捷盘将在下一阶段加入")
            TapSequenceAction.RECENT_ACTION -> playRecent()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        player = GesturePlayer(this)
        scope.launch {
            UserPreferences(applicationContext).bubbleEnabled
                .distinctUntilChanged()
                .collect { enabled -> if (enabled) showBubble() else hideBubble() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        tapCount = 0
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideBubble()
        scope.cancel()
        super.onDestroy()
    }

    private fun showBubble() {
        if (bubble != null) return

        val size = 56.dp
        val view = BubbleView().apply {
            text = "→"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "复刻球，单击执行向右滑动"
            elevation = 8.dp.toFloat()
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(103, 80, 164))
            }
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - 12.dp
            y = resources.displayMetrics.heightPixels / 2
        }

        view.setOnTouchListener(BubbleTouchListener(params))
        windowManager.addView(view, params)
        bubble = view
        bubbleParams = params
    }

    private fun hideBubble() {
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        bubbleParams = null
    }

    private fun registerTap() {
        tapCount += 1
        handler.removeCallbacks(resolveTaps)
        if (tapCount >= 3) {
            resolveTaps.run()
        } else {
            handler.postDelayed(resolveTaps, MULTI_TAP_WINDOW_MS)
        }
    }

    private fun playDefault() {
        player.play(BuiltInActions.swipeRight) { success ->
            if (!success) toast("手势执行失败，请重试")
        }
    }

    private fun playRecent() {
        // Until recording is implemented, the built-in swipe is also the latest action.
        player.play(BuiltInActions.swipeRight) { success ->
            if (!success) toast("最近动作执行失败")
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private inner class BubbleTouchListener(
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private val touchSlop = ViewConfiguration.get(this@GestureAccessibilityService).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false
        private var longPressed = false
        private val longPress = Runnable {
            longPressed = true
            toast("按住并拖动选择动作的快捷盘将在下一阶段加入")
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    longPressed = false
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && dx * dx + dy * dy > touchSlop * touchSlop) {
                        dragging = true
                        handler.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    if (!dragging && !longPressed) view.performClick()
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    return true
                }
            }
            return false
        }
    }

    private inner class BubbleView : TextView(this@GestureAccessibilityService) {
        override fun performClick(): Boolean {
            super.performClick()
            registerTap()
            return true
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private companion object {
        const val MULTI_TAP_WINDOW_MS = 280L
    }
}
