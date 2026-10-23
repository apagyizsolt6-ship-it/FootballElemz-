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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** true = top 5 vagy kedvenc liga → auto AI */
    val autoAiEligible: Boolean = false,
    val headToHead: com.footballai.analyzer.domain.model.HeadToHeadData = com.footballai.analyzer.domain.model.HeadToHeadData(),
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

    init {
        loadAll()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val matchResult = repository.getMatchById(matchId, forceRefresh = false)
            if (matchResult.isFailure) {
                _uiState.value = MatchDetailUiState(
                    isLoading = false,
                    error = matchResult.exceptionOrNull()?.message ?: "Hiba"
                )
                return@launch
            }

            val match = matchResult.getOrThrow()
            val matchForHint = matchResult.getOrNull()
            val extra = repository.getMatchExtraData(
                matchId = matchId,
                forceRefresh = false,
                isLiveHint = matchForHint?.isLive == true
            )
            val h2h = repository.getHeadToHeadData(match.homeTeamId, match.awayTeamId)

            val header = LeagueTranslator.formatHeader(match.countryName, match.leagueName)
            val pinned = userPreferences.pinnedLeagues.first()
            val isTop = LeagueTranslator.isTopLeague(header)
            val isPinned = pinned.contains(header)
            val autoEligible = isTop || isPinned

            // Cache-ből azonnal
            val cached = aiCache.get(matchId)

            _uiState.value = MatchDetailUiState(
                isLoading = false,
                match = match,
                extra = extra,
                headToHead = h2h,
                autoAiEligible = autoEligible,
                aiAnalysis = cached
            )

            // Auto AI: top 5 vagy kedvenc, és még nincs cache
            if (autoEligible && cached == null) {
                generateAiAnalysis(fromAuto = true)
            }
        }
    }

    /**
     * Manuális gomb VAGY auto hívás.
     * @param force true = újragenerálás cache ellenére
     */
    fun generateAiAnalysis(fromAuto: Boolean = false, force: Boolean = false) {
        val match = _uiState.value.match ?: return
        val extra = _uiState.value.extra

        if (!force) {
            aiCache.get(matchId)?.let { cached ->
                _uiState.value = _uiState.value.copy(
                    aiAnalysis = cached,
                    isAiLoading = false,
                    aiError = null,
                    selectedTab = if (fromAuto) _uiState.value.selectedTab else 5
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAiLoading = true,
                aiError = null,
                selectedTab = if (fromAuto) _uiState.value.selectedTab else 5
            )

            val result = llmService.analyzeMatch(match, extra)
            if (result.isSuccess) {
                val text = result.getOrThrow()
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
}
