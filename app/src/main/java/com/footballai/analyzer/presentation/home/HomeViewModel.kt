package com.footballai.analyzer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.local.UserPreferences
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.util.LeagueTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class MatchFilter { ALL, LIVE, FINISHED, UPCOMING }

data class LeagueGroup(
    val leagueName: String,
    val matches: List<Match>,
    val isPinned: Boolean = false
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val filter: MatchFilter = MatchFilter.ALL,
    val onlyTopLeagues: Boolean = false,
    val pinnedLeagues: Set<String> = emptySet(),
    val leagueGroups: List<LeagueGroup> = emptyList(),
    val totalMatchCount: Int = 0,
    val filteredMatchCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allMatches: List<Match> = emptyList()

    init {
        viewModelScope.launch {
            userPreferences.pinnedLeagues.collect { pinned ->
                _uiState.update { it.copy(pinnedLeagues = pinned) }
                applyFilterAndGroup()
            }
        }
        loadMatches()
    }

    fun selectDate(date: LocalDate) {
        if (date == _uiState.value.selectedDate) return
        _uiState.update { it.copy(selectedDate = date) }
        loadMatches()
    }

    fun setFilter(filter: MatchFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilterAndGroup()
    }

    fun toggleTopLeagues() {
        _uiState.update { it.copy(onlyTopLeagues = !it.onlyTopLeagues) }
        applyFilterAndGroup()
    }

    fun togglePin(leagueHeader: String) {
        viewModelScope.launch {
            userPreferences.togglePinned(leagueHeader)
        }
    }

    fun loadMatches(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null
                )
            }
            val result = repository.getMatchesForDate(_uiState.value.selectedDate)
            if (result.isSuccess) {
                allMatches = result.getOrDefault(emptyList())
                applyFilterAndGroup()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        totalMatchCount = allMatches.size
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        totalMatchCount = 0,
                        filteredMatchCount = 0,
                        error = result.exceptionOrNull()?.message ?: "Hiba a betöltéskor"
                    )
                }
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

        val pinned = _uiState.value.pinnedLeagues

        val groups = filtered
            .groupBy { match ->
                LeagueTranslator.formatHeader(match.countryName, match.leagueName)
            }
            .map { (header, matches) ->
                LeagueGroup(
                    leagueName = header,
                    matches = matches.sortedBy { it.date },
                    isPinned = pinned.contains(header)
                )
            }
            .let { list ->
                if (_uiState.value.onlyTopLeagues) {
                    list.filter {
                        it.isPinned || LeagueTranslator.isTopLeague(it.leagueName)
                    }
                } else {
                    list
                }
            }
            .sortedWith(
                compareByDescending<LeagueGroup> { it.isPinned }
                    .thenBy { LeagueTranslator.topLeaguePriority(it.leagueName) }
                    .thenComparator { a, b ->
                        LeagueTranslator.compareHeaders(a.leagueName, b.leagueName)
                    }
            )

        _uiState.update {
            it.copy(
                leagueGroups = groups,
                filteredMatchCount = groups.sumOf { g -> g.matches.size },
                totalMatchCount = allMatches.size
            )
        }
    }
}
