package com.gesturedot.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gestureDotDataStore by preferencesDataStore(name = "gesture_dot_settings")

class UserPreferences(private val context: Context) {
    val bubbleEnabled: Flow<Boolean> = context.gestureDotDataStore.data.map { preferences ->
        preferences[BUBBLE_ENABLED] ?: true
    }

    val hasAcceptedAccessibilityDisclosure: Flow<Boolean> =
        context.gestureDotDataStore.data.map { preferences ->
            preferences[ACCESSIBILITY_DISCLOSURE_ACCEPTED] ?: false
        }

    val permissionOnboardingSeen: Flow<Boolean> =
        context.gestureDotDataStore.data.map { preferences ->
            preferences[PERMISSION_ONBOARDING_SEEN] ?: false
        }

    suspend fun setBubbleEnabled(enabled: Boolean) {
        context.gestureDotDataStore.edit { preferences ->
            preferences[BUBBLE_ENABLED] = enabled
        }
    }

    suspend fun acceptAccessibilityDisclosure() {
        context.gestureDotDataStore.edit { preferences ->
            preferences[ACCESSIBILITY_DISCLOSURE_ACCEPTED] = true
            preferences[PERMISSION_ONBOARDING_SEEN] = true
        }
    }

    suspend fun markPermissionOnboardingSeen() {
        context.gestureDotDataStore.edit { preferences ->
            preferences[PERMISSION_ONBOARDING_SEEN] = true
        }
    }

    private companion object {
        val BUBBLE_ENABLED = booleanPreferencesKey("bubble_enabled")
        val ACCESSIBILITY_DISCLOSURE_ACCEPTED =
            booleanPreferencesKey("accessibility_disclosure_accepted")
        val PERMISSION_ONBOARDING_SEEN = booleanPreferencesKey("permission_onboarding_seen")
    }
}
