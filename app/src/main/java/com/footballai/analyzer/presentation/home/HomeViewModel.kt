package com.footballai.analyzer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.local.AiAnalysisCache
import com.footballai.analyzer.data.local.UserPreferences
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.util.LeagueTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val pinnedLeagues: Set<String> = emptySet(),
    val favoriteTeams: Set<String> = emptySet(),
    val leagueGroups: List<LeagueGroup> = emptyList(),
    val totalMatchCount: Int = 0,
    val filteredMatchCount: Int = 0,
    val hasLiveOnSelectedDate: Boolean = false,
    val error: String? = null,
    val lastUpdatedLabel: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val userPreferences: UserPreferences,
    private val aiCache: AiAnalysisCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allMatches: List<Match> = emptyList()
    private var livePollJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferences.pinnedLeagues.collect { pinned ->
                _uiState.update { it.copy(pinnedLeagues = pinned) }
                applyFilterAndGroup()
            }
        }
        viewModelScope.launch {
            userPreferences.favoriteTeams.collect { favs ->
                _uiState.update { it.copy(favoriteTeams = favs) }
                applyFilterAndGroup()
            }
        }
        loadMatches()
    }

    fun aiPreview(matchId: Long): String? = aiCache.get(matchId)?.lineSequence()?.firstOrNull()?.trim()

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

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
        applyFilterAndGroup()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndGroup()
    }

    fun togglePin(leagueHeader: String) {
        viewModelScope.launch { userPreferences.togglePinned(leagueHeader) }
    }

    fun toggleFavoriteTeam(teamName: String) {
        viewModelScope.launch { userPreferences.toggleFavoriteTeam(teamName) }
    }

    fun loadMatches(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null)
            }
            val result = repository.getMatchesForDate(
                _uiState.value.selectedDate,
                forceRefresh = isRefresh
            )
            if (result.isSuccess) {
                allMatches = result.getOrDefault(emptyList())
                applyFilterAndGroup()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        totalMatchCount = allMatches.size,
                        lastUpdatedLabel = "Frissítve " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                        hasLiveOnSelectedDate = allMatches.any { m -> m.isLive }
                    )
                }
                restartLivePolling()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.exceptionOrNull()?.message ?: "Nem sikerült betölteni a meccseket."
                    )
                }
            }
        }
    }

    private fun restartLivePolling() {
        livePollJob?.cancel()
        if (allMatches.none { it.isLive }) return
        livePollJob = viewModelScope.launch {
            while (isActive) {
                delay(50_000L)
                if (allMatches.none { it.isLive }) break
                val result = repository.getMatchesForDate(
                    _uiState.value.selectedDate,
                    forceRefresh = true
                )
                if (result.isSuccess) {
                    allMatches = result.getOrDefault(emptyList())
                    applyFilterAndGroup()
                    _uiState.update {
                        it.copy(
                            totalMatchCount = allMatches.size,
                            hasLiveOnSelectedDate = allMatches.any { m -> m.isLive }
                        )
                    }
                    if (allMatches.none { it.isLive }) break
                }
            }
        }
    }

    private fun applyFilterAndGroup() {
        val state = _uiState.value
        var list = allMatches

        when (state.filter) {
            MatchFilter.ALL -> {}
            MatchFilter.LIVE -> list = list.filter { it.isLive }
            MatchFilter.FINISHED -> list = list.filter {
                val s = it.status.lowercase()
                s.contains("finished") || s == "ft"
            }
            MatchFilter.UPCOMING -> list = list.filter {
                !it.isLive && !it.status.lowercase().contains("finished") && it.status.lowercase() != "ft"
            }
        }

        val q = state.searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter {
                it.homeTeamName.lowercase().contains(q) ||
                    it.awayTeamName.lowercase().contains(q) ||
                    it.leagueName.lowercase().contains(q) ||
                    (it.countryName?.lowercase()?.contains(q) == true)
            }
        }

        if (state.onlyFavorites && state.favoriteTeams.isNotEmpty()) {
            list = list.filter {
                it.homeTeamName in state.favoriteTeams || it.awayTeamName in state.favoriteTeams
            }
        }

        val keyed = list.map { m ->
            LeagueTranslator.formatHeader(m.countryName, m.leagueName) to m
        }

        var groups = keyed.groupBy { it.first }.map { (header, items) ->
            LeagueGroup(
                leagueName = header,
                matches = items.map { it.second },
                isPinned = header in state.pinnedLeagues
            )
        }

        if (state.onlyTopLeagues) {
            groups = groups.filter {
                LeagueTranslator.isTopLeague(it.leagueName) || it.isPinned
            }
        }

        groups = groups.sortedWith { a, b ->
            val ap = if (a.isPinned) 0 else 1
            val bp = if (b.isPinned) 0 else 1
            if (ap != bp) return@sortedWith ap - bp
            val at = LeagueTranslator.topLeaguePriority(a.leagueName)
            val bt = LeagueTranslator.topLeaguePriority(b.leagueName)
            if (at != bt) return@sortedWith at - bt
            LeagueTranslator.compareHeaders(a.leagueName, b.leagueName)
        }

        _uiState.update {
            it.copy(
                leagueGroups = groups,
                filteredMatchCount = groups.sumOf { g -> g.matches.size }
            )
        }
    }
}
