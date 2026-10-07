package com.footballai.analyzer.presentation.matchdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val error: String? = null
)

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val matchId: Long = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow(MatchDetailUiState())
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        loadMatch()
    }

    fun loadMatch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getMatchById(matchId)
                .onSuccess { match ->
                    _uiState.value = MatchDetailUiState(
                        isLoading = false,
                        match = match
                    )
                }
                .onFailure { e ->
                    _uiState.value = MatchDetailUiState(
                        isLoading = false,
                        error = e.message ?: "Ismeretlen hiba"
                    )
                }
        }
    }
}
