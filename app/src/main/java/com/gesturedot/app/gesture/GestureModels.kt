package com.gesturedot.app.gesture

/** A display-independent coordinate, where both axes are in the inclusive 0..1 range. */
data class NormalizedPoint(
    val x: Float,
    val y: Float,
) {
    init {
        require(x in 0f..1f) { "x must be normalized" }
        require(y in 0f..1f) { "y must be normalized" }
    }
}

sealed interface GestureStep {
    val delayAfterMs: Long

    data class Swipe(
        val points: List<NormalizedPoint>,
        val durationMs: Long,
        override val delayAfterMs: Long = 0,
    ) : GestureStep {
        init {
            require(points.size >= 2) { "A swipe needs at least two points" }
            require(durationMs in 1..60_000) { "Invalid swipe duration" }
            require(delayAfterMs >= 0) { "Delay cannot be negative" }
        }
    }

    data class Tap(
        val point: NormalizedPoint,
        val holdMs: Long = 80,
        override val delayAfterMs: Long = 0,
    ) : GestureStep
}

data class GestureAction(
    val id: String,
    val name: String,
    val steps: List<GestureStep>,
)

object BuiltInActions {
    val swipeRight = GestureAction(
        id = "builtin-swipe-right",
        name = "向右滑动",
        steps = listOf(
            GestureStep.Swipe(
                points = listOf(
                    NormalizedPoint(0.22f, 0.55f),
                    NormalizedPoint(0.78f, 0.55f),
                ),
                durationMs = 360,
            ),
        ),
    )
}

