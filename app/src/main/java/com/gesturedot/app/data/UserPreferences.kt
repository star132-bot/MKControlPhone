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

    suspend fun setBubbleEnabled(enabled: Boolean) {
        context.gestureDotDataStore.edit { preferences ->
            preferences[BUBBLE_ENABLED] = enabled
        }
    }

    private companion object {
        val BUBBLE_ENABLED = booleanPreferencesKey("bubble_enabled")
    }
}

