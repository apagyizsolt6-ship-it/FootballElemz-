package com.footballai.analyzer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.local.AiAnalysisCache
import com.footballai.analyzer.data.local.ApiKeyProvider
import com.footballai.analyzer.data.local.MatchLocalCache
import com.footballai.analyzer.data.local.UserPreferences
import com.footballai.analyzer.fcm.FcmRegistrar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val llmKey: String = "",
    val llmBaseUrl: String = "https://api.x.ai/v1/",
    val llmModel: String = "grok-4.7",
    val fcmServerUrl: String = "",
    val fcmServerSecret: String = "",
    val favoriteTeams: List<String> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
    private val matchCache: MatchLocalCache,
    private val aiCache: AiAnalysisCache,
    private val userPreferences: UserPreferences,
    private val fcmRegistrar: FcmRegistrar
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                apiKey = apiKeyProvider.getApiKey(),
                llmKey = apiKeyProvider.getLlmKey(),
                llmBaseUrl = apiKeyProvider.getLlmBaseUrl(),
                llmModel = apiKeyProvider.getLlmModel(),
                fcmServerUrl = apiKeyProvider.getFcmServerUrl(),
                fcmServerSecret = apiKeyProvider.getFcmServerSecret()
            )
        }
        viewModelScope.launch {
            userPreferences.favoriteTeams.collect { favs ->
                _uiState.value = _uiState.value.copy(favoriteTeams = favs.sorted())
            }
        }
    }

    fun onApiKeyChange(v: String) { _uiState.value = _uiState.value.copy(apiKey = v, message = null) }
    fun onLlmKeyChange(v: String) { _uiState.value = _uiState.value.copy(llmKey = v, message = null) }
    fun onLlmBaseUrlChange(v: String) { _uiState.value = _uiState.value.copy(llmBaseUrl = v, message = null) }
    fun onLlmModelChange(v: String) { _uiState.value = _uiState.value.copy(llmModel = v, message = null) }
    fun onFcmServerUrlChange(v: String) { _uiState.value = _uiState.value.copy(fcmServerUrl = v, message = null) }
    fun onFcmServerSecretChange(v: String) { _uiState.value = _uiState.value.copy(fcmServerSecret = v, message = null) }

    fun saveAll() {
        viewModelScope.launch {
            apiKeyProvider.saveApiKey(_uiState.value.apiKey)
            apiKeyProvider.saveLlmKey(_uiState.value.llmKey)
            apiKeyProvider.saveLlmBaseUrl(_uiState.value.llmBaseUrl)
            apiKeyProvider.saveLlmModel(_uiState.value.llmModel)
            apiKeyProvider.saveFcmServerUrl(_uiState.value.fcmServerUrl)
            apiKeyProvider.saveFcmServerSecret(_uiState.value.fcmServerSecret)
            fcmRegistrar.registerInBackground(viewModelScope)
            _uiState.value = _uiState.value.copy(message = "Mentve · FCM regisztráció indítva")
        }
    }

    fun clearCaches() {
        viewModelScope.launch {
            matchCache.clearAll()
            aiCache.clear()
            _uiState.value = _uiState.value.copy(message = "Cache törölve")
        }
    }

    fun removeFavorite(teamName: String) {
        viewModelScope.launch {
            userPreferences.toggleFavoriteTeam(teamName)
        }
    }
}
