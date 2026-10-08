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
    private val highlightlyKey = stringPreferencesKey("highlightly_api_key")
    private val llmKey = stringPreferencesKey("llm_api_key")
    private val llmBaseUrl = stringPreferencesKey("llm_base_url")
    private val llmModel = stringPreferencesKey("llm_model")

    // --- Highlightly ---
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { it[highlightlyKey] = apiKey.trim() }
    }

    suspend fun getApiKey(): String =
        context.dataStore.data.first()[highlightlyKey] ?: ""

    fun getApiKeyBlocking(): String = runBlocking { getApiKey() }

    // --- LLM ---
    suspend fun saveLlmKey(key: String) {
        context.dataStore.edit { it[llmKey] = key.trim() }
    }

    suspend fun getLlmKey(): String =
        context.dataStore.data.first()[llmKey] ?: ""

    suspend fun saveLlmBaseUrl(url: String) {
        context.dataStore.edit { it[llmBaseUrl] = url.trim() }
    }

    suspend fun getLlmBaseUrl(): String =
        context.dataStore.data.first()[llmBaseUrl]
            ?: "https://api.openai.com/v1/"

    suspend fun saveLlmModel(model: String) {
        context.dataStore.edit { it[llmModel] = model.trim() }
    }

    suspend fun getLlmModel(): String =
        context.dataStore.data.first()[llmModel] ?: "gpt-4o-mini"
}
