package com.gesturedot.app.gesture

enum class TapSequenceAction {
    DEFAULT_ACTION,
    OPEN_QUICK_RING,
    RECENT_ACTION,
}

object TapSequenceClassifier {
    fun classify(tapCount: Int): TapSequenceAction = when {
        tapCount >= 3 -> TapSequenceAction.RECENT_ACTION
        tapCount == 2 -> TapSequenceAction.OPEN_QUICK_RING
        else -> TapSequenceAction.DEFAULT_ACTION
    }
}

