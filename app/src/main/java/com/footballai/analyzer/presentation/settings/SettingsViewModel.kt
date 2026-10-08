package com.footballai.analyzer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.local.ApiKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val llmKey: String = "",
    val llmBaseUrl: String = "https://api.openai.com/v1/",
    val llmModel: String = "gpt-4o-mini",
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                apiKey = apiKeyProvider.getApiKey(),
                llmKey = apiKeyProvider.getLlmKey(),
                llmBaseUrl = apiKeyProvider.getLlmBaseUrl(),
                llmModel = apiKeyProvider.getLlmModel()
            )
        }
    }

    fun onApiKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value, message = null)
    }

    fun onLlmKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(llmKey = value, message = null)
    }

    fun onLlmBaseUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(llmBaseUrl = value, message = null)
    }

    fun onLlmModelChange(value: String) {
        _uiState.value = _uiState.value.copy(llmModel = value, message = null)
    }

    fun saveAll() {
        viewModelScope.launch {
            apiKeyProvider.saveApiKey(_uiState.value.apiKey)
            apiKeyProvider.saveLlmKey(_uiState.value.llmKey)
            apiKeyProvider.saveLlmBaseUrl(_uiState.value.llmBaseUrl)
            apiKeyProvider.saveLlmModel(_uiState.value.llmModel)
            _uiState.value = _uiState.value.copy(message = "Minden beállítás elmentve")
        }
    }
}
