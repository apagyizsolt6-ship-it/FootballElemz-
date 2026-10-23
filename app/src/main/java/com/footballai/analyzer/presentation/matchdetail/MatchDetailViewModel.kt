package com.footballai.analyzer.presentation.matchdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.local.AiAnalysisCache
import com.footballai.analyzer.data.local.UserPreferences
import com.footballai.analyzer.data.remote.LlmService
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
import com.footballai.analyzer.util.LeagueTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val extra: MatchExtraData = MatchExtraData(),
    val selectedTab: Int = 0,
    val aiAnalysis: String? = null,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val autoAiEligible: Boolean = false,
    val headToHead: com.footballai.analyzer.domain.model.HeadToHeadData =
        com.footballai.analyzer.domain.model.HeadToHeadData(),
    val favoriteTeams: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val llmService: LlmService,
    private val userPreferences: UserPreferences,
    private val aiCache: AiAnalysisCache,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val matchId: Long = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow(MatchDetailUiState())
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private var livePollJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferences.favoriteTeams.collect { favs ->
                _uiState.value = _uiState.value.copy(favoriteTeams = favs)
            }
        }
        loadAll()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadAll(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val matchResult = repository.getMatchById(matchId, forceRefresh = silent)
            if (matchResult.isFailure) {
                if (!silent) {
                    _uiState.value = MatchDetailUiState(
                        isLoading = false,
                        error = matchResult.exceptionOrNull()?.message ?: "Hiba"
                    )
                }
                return@launch
            }

            val match = matchResult.getOrThrow()
            val extra = repository.getMatchExtraData(
                matchId = matchId,
                forceRefresh = silent || match.isLive,
                isLiveHint = match.isLive
            )
            val h2h = if (silent) {
                _uiState.value.headToHead
            } else {
                repository.getHeadToHeadData(match.homeTeamId, match.awayTeamId)
            }

            val header = LeagueTranslator.formatHeader(match.countryName, match.leagueName)
            val pinned = userPreferences.pinnedLeagues.first()
            val isTop = LeagueTranslator.isTopLeague(header)
            val isPinned = pinned.contains(header)
            val autoEligible = isTop || isPinned
            val cached = aiCache.get(matchId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                match = match,
                extra = extra,
                headToHead = h2h,
                autoAiEligible = autoEligible,
                aiAnalysis = _uiState.value.aiAnalysis ?: cached,
                error = null
            )

            if (!silent && autoEligible && cached == null && _uiState.value.aiAnalysis == null) {
                generateAiAnalysis(fromAuto = true)
            }

            restartLivePolling(match.isLive)
        }
    }

    private fun restartLivePolling(isLive: Boolean) {
        livePollJob?.cancel()
        if (!isLive) return
        livePollJob = viewModelScope.launch {
            while (isActive) {
                delay(45_000L)
                val current = _uiState.value.match
                if (current?.isLive != true) break
                loadAll(silent = true)
            }
        }
    }

    fun generateAiAnalysis(fromAuto: Boolean = false, force: Boolean = false) {
        val match = _uiState.value.match ?: return
        if (_uiState.value.isAiLoading) return
        if (!force && _uiState.value.aiAnalysis != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true, aiError = null)
            val result = llmService.analyzeMatch(match, _uiState.value.extra)
            if (result.isSuccess) {
                val text = result.getOrDefault("")
                aiCache.put(matchId, text)
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    aiAnalysis = text,
                    aiError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    aiError = result.exceptionOrNull()?.message ?: "AI hiba"
                )
            }
        }
    }

    fun regenerateAi() {
        generateAiAnalysis(fromAuto = false, force = true)
    }

    fun toggleFavoriteTeam(teamName: String) {
        viewModelScope.launch {
            userPreferences.toggleFavoriteTeam(teamName)
        }
    }
}
