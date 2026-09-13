package com.gesturedot.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class TapSequenceClassifierTest {
    @Test
    fun singleTapRunsDefaultAction() {
        assertEquals(TapSequenceAction.DEFAULT_ACTION, TapSequenceClassifier.classify(1))
    }

    @Test
    fun doubleTapOpensQuickRing() {
        assertEquals(TapSequenceAction.OPEN_QUICK_RING, TapSequenceClassifier.classify(2))
    }

    @Test
    fun tripleTapRunsRecentAction() {
        assertEquals(TapSequenceAction.RECENT_ACTION, TapSequenceClassifier.classify(3))
    }
}

