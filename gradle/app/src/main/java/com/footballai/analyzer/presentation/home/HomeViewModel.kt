package com.footballai.analyzer.presentation.home

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

data class HomeUiState(
    val isLoading: Boolean = false,
    val liveMatches: List<Match> = emptyList(),
    val todayMatches: List<Match> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val liveResult = repository.getLiveMatches()
            val todayResult = repository.getTodayMatches()

            if (liveResult.isSuccess && todayResult.isSuccess) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    liveMatches = liveResult.getOrDefault(emptyList()),
                    todayMatches = todayResult.getOrDefault(emptyList())
                )
            } else {
                val errorMsg = liveResult.exceptionOrNull()?.message
                    ?: todayResult.exceptionOrNull()?.message
                    ?: "Ismeretlen hiba"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }
}
