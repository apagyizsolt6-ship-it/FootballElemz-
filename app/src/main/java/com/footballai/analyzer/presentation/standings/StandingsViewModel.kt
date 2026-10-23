package com.footballai.analyzer.presentation.standings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.StandingsGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StandingsUiState(
    val isLoading: Boolean = true,
    val title: String = "Tabella",
    val groups: List<StandingsGroup> = emptyList(),
    val highlightTeamIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val repository: MatchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val leagueId: Long = savedStateHandle["leagueId"] ?: 0L
    private val season: Int = savedStateHandle["season"] ?: 0
    private val titleArg: String = savedStateHandle["title"] ?: "Tabella"
    private val homeId: Long = savedStateHandle["homeId"] ?: 0L
    private val awayId: Long = savedStateHandle["awayId"] ?: 0L

    private val _uiState = MutableStateFlow(
        StandingsUiState(
            title = titleArg,
            highlightTeamIds = buildSet {
                if (homeId > 0) add(homeId)
                if (awayId > 0) add(awayId)
            }
        )
    )
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (leagueId <= 0 || season <= 0) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Nincs liga azonosító ehhez a bajnoksághoz.")
                }
                return@launch
            }
            val result = repository.getStandings(leagueId, season)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, groups = result.getOrDefault(emptyList()))
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Tabella nem elérhető"
                    )
                }
            }
        }
    }
}
