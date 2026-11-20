package com.example.data.firebase

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vibeDataStore by preferencesDataStore(name = "vibeplay_settings")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ROOM_EFFECTS = booleanPreferencesKey("room_effects")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val RECENT_ROOMS = stringPreferencesKey("recent_room_ids")
    }

    val darkThemeFlow: Flow<Boolean> = context.vibeDataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    val roomEffectsFlow: Flow<Boolean> = context.vibeDataStore.data.map { prefs ->
        prefs[Keys.ROOM_EFFECTS] ?: true
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.vibeDataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val recentRoomIdsFlow: Flow<List<String>> = context.vibeDataStore.data.map { prefs ->
        prefs[Keys.RECENT_ROOMS]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.vibeDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setRoomEffects(enabled: Boolean) {
        context.vibeDataStore.edit { it[Keys.ROOM_EFFECTS] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.vibeDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun recordRecentRoom(roomId: String) {
        if (roomId.isBlank()) return
        context.vibeDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_ROOMS]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            val updated = (listOf(roomId) + current.filter { it != roomId }).take(10)
            prefs[Keys.RECENT_ROOMS] = updated.joinToString(",")
        }
    }
}
