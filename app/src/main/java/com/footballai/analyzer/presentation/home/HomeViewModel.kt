package com.footballai.analyzer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.util.LeagueTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class MatchFilter { ALL, LIVE, FINISHED, UPCOMING }

data class LeagueGroup(
    val leagueName: String,
    val matches: List<Match>
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val filter: MatchFilter = MatchFilter.ALL,
    val onlyTopLeagues: Boolean = false,
    val leagueGroups: List<LeagueGroup> = emptyList(),
    val totalMatchCount: Int = 0,
    val filteredMatchCount: Int = 0,
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

    fun toggleTopLeagues() {
        _uiState.value = _uiState.value.copy(
            onlyTopLeagues = !_uiState.value.onlyTopLeagues
        )
        applyFilterAndGroup()
    }

    fun loadMatches(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                error = null
            )
            val result = repository.getMatchesForDate(_uiState.value.selectedDate)
            if (result.isSuccess) {
                allMatches = result.getOrDefault(emptyList())
                applyFilterAndGroup()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    totalMatchCount = allMatches.size
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    totalMatchCount = 0,
                    filteredMatchCount = 0,
                    error = result.exceptionOrNull()?.message ?: "Hiba a betöltéskor"
                )
            }
        }
    }

    private fun applyFilterAndGroup() {
        var filtered = when (_uiState.value.filter) {
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
            .groupBy { match ->
                LeagueTranslator.formatHeader(match.countryName, match.leagueName)
            }
            .map { (header, matches) ->
                LeagueGroup(header, matches.sortedBy { it.date })
            }
            .let { list ->
                if (_uiState.value.onlyTopLeagues) {
                    list.filter { LeagueTranslator.isTopLeague(it.leagueName) }
                } else {
                    list
                }
            }
            // Top 5 mindig elöl, utána ABC
            .sortedWith(
                compareBy<LeagueGroup> { LeagueTranslator.topLeaguePriority(it.leagueName) }
                    .thenBy { it.leagueName }
            )

        _uiState.value = _uiState.value.copy(
            leagueGroups = groups,
            filteredMatchCount = groups.sumOf { it.matches.size },
            totalMatchCount = allMatches.size
        )
    }
}
