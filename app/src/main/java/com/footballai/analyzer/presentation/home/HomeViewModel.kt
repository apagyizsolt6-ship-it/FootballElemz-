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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class MatchFilter { ALL, LIVE, FINISHED, UPCOMING }

data class LeagueGroup(
    val leagueName: String,
    val matches: List<Match>
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val filter: MatchFilter = MatchFilter.ALL,
    val leagueGroups: List<LeagueGroup> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allMatches: List<Match> = emptyList()

    init {
        loadMatches()
    }

    fun selectDate(date: LocalDate) {
        if (date == _uiState.value.selectedDate) return
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadMatches()
    }

    fun setFilter(filter: MatchFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        applyFilterAndGroup()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getMatchesForDate(_uiState.value.selectedDate)
            if (result.isSuccess) {
                allMatches = result.getOrDefault(emptyList())
                applyFilterAndGroup()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Hiba a betöltéskor"
                )
            }
        }
    }

    private fun applyFilterAndGroup() {
        val filtered = when (_uiState.value.filter) {
            MatchFilter.ALL -> allMatches
            MatchFilter.LIVE -> allMatches.filter { it.isLive }
            MatchFilter.FINISHED -> allMatches.filter {
                it.status.contains("Finished", ignoreCase = true) ||
                it.status.contains("Vége", ignoreCase = true) ||
                it.status == "Finished after penalties" ||
                it.status == "Finished after extra time"
            }
            MatchFilter.UPCOMING -> allMatches.filter {
                it.status == "Not started" || it.status == "To be announced"
            }
        }

        val groups = filtered
            .groupBy { it.leagueName.ifBlank { "Egyéb" } }
            .map { (league, matches) ->
                LeagueGroup(league, matches.sortedBy { it.date })
            }
            .sortedBy { it.leagueName }

        _uiState.value = _uiState.value.copy(leagueGroups = groups)
    }
}
