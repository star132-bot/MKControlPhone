package com.gesturedot.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionOnboardingTest {
    @Test
    fun waitsUntilPreferencesAreLoaded() {
        assertFalse(shouldShowPermissionOnboarding(onboardingSeen = null, serviceEnabled = false))
    }

    @Test
    fun showsForFirstLaunchWhenServiceIsDisabled() {
        assertTrue(shouldShowPermissionOnboarding(onboardingSeen = false, serviceEnabled = false))
    }

    @Test
    fun doesNotRepeatAfterOnboardingWasSeen() {
        assertFalse(shouldShowPermissionOnboarding(onboardingSeen = true, serviceEnabled = false))
    }

    @Test
    fun doesNotShowWhenServiceIsAlreadyEnabled() {
        assertFalse(shouldShowPermissionOnboarding(onboardingSeen = false, serviceEnabled = true))
    }
}
