package com.footballai.analyzer.presentation.matchdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.remote.LlmService
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val extra: MatchExtraData = MatchExtraData(),
    val selectedTab: Int = 0,
    val aiAnalysis: String? = null,
    val isAiLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val llmService: LlmService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val matchId: Long = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow(MatchDetailUiState())
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val matchResult = repository.getMatchById(matchId)
            if (matchResult.isFailure) {
                _uiState.value = MatchDetailUiState(
                    isLoading = false,
                    error = matchResult.exceptionOrNull()?.message ?: "Hiba"
                )
                return@launch
            }

            val match = matchResult.getOrThrow()
            val extra = repository.getMatchExtraData(matchId)

            _uiState.value = MatchDetailUiState(
                isLoading = false,
                match = match,
                extra = extra
            )
        }
    }

    fun generateAiAnalysis() {
        val match = _uiState.value.match ?: return
        val extra = _uiState.value.extra

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true, selectedTab = 4, aiAnalysis = null)

            val result = llmService.analyzeMatch(match, extra)

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isAiLoading = false,
                    aiAnalysis = result.getOrThrow()
                )
            } else {
                _uiState.value.copy(
                    isAiLoading = false,
                    aiAnalysis = "❌ LLM hiba:\n${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
}
