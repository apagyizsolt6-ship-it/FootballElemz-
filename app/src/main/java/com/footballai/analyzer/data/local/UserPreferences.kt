package com.footballai.analyzer.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefs by preferencesDataStore(name = "focielemzo_user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pinnedKey = stringSetPreferencesKey("pinned_leagues")
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    val pinnedLeagues: Flow<Set<String>> = context.userPrefs.data.map { prefs ->
        prefs[pinnedKey] ?: emptySet()
    }

    val isDarkTheme: Flow<Boolean> = context.userPrefs.data.map { prefs ->
        prefs[darkThemeKey] ?: true // alapbol sotet
    }

    suspend fun togglePinned(leagueHeader: String) {
        context.userPrefs.edit { prefs ->
            val current = prefs[pinnedKey]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(leagueHeader)) {
                current.remove(leagueHeader)
            } else {
                current.add(leagueHeader)
            }
            prefs[pinnedKey] = current
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.userPrefs.edit { prefs ->
            prefs[darkThemeKey] = dark
        }
    }
}
