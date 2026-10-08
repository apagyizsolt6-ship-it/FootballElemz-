package com.footballai.analyzer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ApiKeyDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKeyKey = stringPreferencesKey("highlightly_api_key")

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[apiKeyKey] ?: ""
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[apiKeyKey] = key.trim()
        }
    }

    suspend fun getApiKey(): String {
        var key = ""
        context.dataStore.data.map { prefs ->
            key = prefs[apiKeyKey] ?: ""
        }
        // Egyszerűbb szinkron olvasás helyett a Flow-t használjuk, de interceptorhoz kell egy blocking megoldás
        return key
    }
}
