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
import com.footballai.analyzer.domain.model.OddsRow
import com.footballai.analyzer.util.InstantAnalyzer
import com.footballai.analyzer.util.LeagueTranslator
import com.footballai.analyzer.util.MatchNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    val isInstantAi: Boolean = false,
    val headToHead: com.footballai.analyzer.domain.model.HeadToHeadData =
        com.footballai.analyzer.domain.model.HeadToHeadData(),
    val favoriteTeams: Set<String> = emptySet(),
    val odds: List<OddsRow> = emptyList(),
    val similarMatches: List<Match> = emptyList(),
    val isWatched: Boolean = false,
    val needsNotificationPermission: Boolean = false,
    val lastUpdatedLabel: String? = null,
    val selectedPlayerName: String? = null,
    val selectedPlayerInfo: String? = null,
    val error: String? = null
)

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val llmService: LlmService,
    private val userPreferences: UserPreferences,
    private val aiCache: AiAnalysisCache,
    private val notifications: MatchNotificationHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val matchId: Long = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow(MatchDetailUiState())
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private var livePollJob: Job? = null
    private var llmJob: Job? = null

    init {
        notifications.ensureChannel()
        viewModelScope.launch {
            userPreferences.favoriteTeams.collect { favs ->
                _uiState.value = _uiState.value.copy(favoriteTeams = favs)
            }
        }
        viewModelScope.launch {
            userPreferences.watchedMatches.collect { set ->
                _uiState.value = _uiState.value.copy(isWatched = matchId.toString() in set)
            }
        }
        loadAll()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
        if (index == 5 && _uiState.value.aiAnalysis == null && _uiState.value.match != null) {
            showInstantAndMaybeLlm(forceLlm = false)
        }
    }

    fun loadAll(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val matchResult = repository.getMatchById(matchId, forceRefresh = silent)
            if (matchResult.isFailure) {
                if (!silent) {
                    _uiState.value = _uiState.value.copy(
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
            val h2h = if (silent) _uiState.value.headToHead
            else repository.getHeadToHeadData(match.homeTeamId, match.awayTeamId)

            val header = LeagueTranslator.formatHeader(match.countryName, match.leagueName)
            val pinned = userPreferences.pinnedLeagues.first()
            val autoEligible = LeagueTranslator.isTopLeague(header) || header in pinned
            val cached = aiCache.get(matchId)
            val odds = if (silent) _uiState.value.odds else repository.getOdds(matchId)
            val similar = if (silent) _uiState.value.similarMatches
            else repository.getSimilarMatches(match)

            val stamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val watched = _uiState.value.isWatched

            // Értesítés állásváltozáskor
            if (watched) {
                notifications.notifyScoreIfChanged(match, watched = true)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                match = match,
                extra = extra,
                headToHead = h2h,
                autoAiEligible = autoEligible,
                aiAnalysis = cached ?: _uiState.value.aiAnalysis,
                odds = odds,
                similarMatches = similar,
                lastUpdatedLabel = "Frissítve $stamp",
                error = null
            )

            if (!silent) {
                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        aiAnalysis = cached,
                        isInstantAi = false,
                        isAiLoading = false
                    )
                } else {
                    showInstantAndMaybeLlm(forceLlm = autoEligible)
                }
            }

            restartLivePolling(match.isLive)
        }
    }

    private fun showInstantAndMaybeLlm(forceLlm: Boolean) {
        val match = _uiState.value.match ?: return
        val instant = InstantAnalyzer.analyze(match, _uiState.value.extra)
        _uiState.value = _uiState.value.copy(
            aiAnalysis = instant,
            isInstantAi = true,
            isAiLoading = forceLlm,
            aiError = null
        )
        if (forceLlm) runLlmInBackground(match, _uiState.value.extra)
    }

    private fun runLlmInBackground(match: Match, extra: MatchExtraData) {
        llmJob?.cancel()
        llmJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            val result = llmService.analyzeMatch(match, extra)
            if (result.isSuccess) {
                val text = result.getOrDefault("")
                if (text.isNotBlank()) {
                    aiCache.put(matchId, text)
                    _uiState.value = _uiState.value.copy(
                        isAiLoading = false,
                        aiAnalysis = text,
                        isInstantAi = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isAiLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isAiLoading = false)
            }
        }
    }

    private fun restartLivePolling(isLive: Boolean) {
        livePollJob?.cancel()
        if (!isLive) return
        livePollJob = viewModelScope.launch {
            while (isActive) {
                delay(45_000L)
                if (_uiState.value.match?.isLive != true) break
                loadAll(silent = true)
            }
        }
    }

    fun generateAiAnalysis(fromAuto: Boolean = false, force: Boolean = false) {
        val match = _uiState.value.match ?: return
        if (_uiState.value.isAiLoading && !force) return
        if (force) {
            runLlmInBackground(match, _uiState.value.extra)
            return
        }
        if (_uiState.value.aiAnalysis != null && !_uiState.value.isInstantAi) return
        showInstantAndMaybeLlm(forceLlm = true)
    }

    fun regenerateAi() = generateAiAnalysis(force = true)

    fun toggleFavoriteTeam(teamName: String) {
        viewModelScope.launch { userPreferences.toggleFavoriteTeam(teamName) }
    }

    /**
     * Értesítés kapcsoló. Ha nincs permission, a UI-nak kell kérnie.
     * @return true ha permission kell
     */
    fun toggleWatchMatch(): Boolean {
        val turningOn = !_uiState.value.isWatched
        if (turningOn && !notifications.hasPermission()) {
            _uiState.value = _uiState.value.copy(needsNotificationPermission = true)
            return true
        }
        viewModelScope.launch {
            userPreferences.toggleWatchedMatch(matchId)
            val match = _uiState.value.match
            if (turningOn && match != null) {
                notifications.notifyWatchEnabled(match)
                notifications.notifyScoreIfChanged(match, watched = true)
            }
        }
        return false
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(needsNotificationPermission = false)
        if (granted) {
            notifications.ensureChannel()
            viewModelScope.launch {
                if (!_uiState.value.isWatched) {
                    userPreferences.toggleWatchedMatch(matchId)
                }
                _uiState.value.match?.let { notifications.notifyWatchEnabled(it) }
            }
        }
    }

    fun clearPermissionRequest() {
        _uiState.value = _uiState.value.copy(needsNotificationPermission = false)
    }

    fun selectPlayer(name: String, number: Int?, position: String?) {
        val info = buildString {
            append(name)
            number?.let { append(" · #$it") }
            position?.let { append(" · $it") }
        }
        _uiState.value = _uiState.value.copy(
            selectedPlayerName = name,
            selectedPlayerInfo = info
        )
    }

    fun clearSelectedPlayer() {
        _uiState.value = _uiState.value.copy(
            selectedPlayerName = null,
            selectedPlayerInfo = null
        )
    }

    fun aiOneLiner(): String? {
        val text = _uiState.value.aiAnalysis ?: return null
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("_") }
            .firstOrNull()
            ?.take(140)
    }

    fun overviewShareText(): String {
        val m = _uiState.value.match ?: return ""
        val ai = aiOneLiner() ?: ""
        return buildString {
            appendLine("${m.homeTeamName} ${m.score} ${m.awayTeamName}")
            appendLine(m.leagueName)
            appendLine(m.status)
            if (ai.isNotBlank()) appendLine(ai)
            appendLine("— FociElemző")
        }
    }
}
