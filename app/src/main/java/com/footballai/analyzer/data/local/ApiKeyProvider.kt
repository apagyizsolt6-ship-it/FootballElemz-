package com.footballai.analyzer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "focielemzo_settings")

@Singleton
class ApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val key = stringPreferencesKey("highlightly_api_key")

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[key] = apiKey.trim()
        }
    }

    suspend fun getApiKey(): String {
        return context.dataStore.data.first()[key] ?: ""
    }

    /** OkHttp interceptorhoz – blocking olvasás */
    fun getApiKeyBlocking(): String {
        return runBlocking { getApiKey() }
    }
}
