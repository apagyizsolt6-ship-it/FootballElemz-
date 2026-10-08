package com.footballai.analyzer.presentation.matchdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.repository.MatchRepository
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val extra: MatchExtraData = MatchExtraData(),
    val selectedTab: Int = 0,
    val aiAnalysis: String? = null,
    val isAiLoading: Boolean = false,
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
        loadAll()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val matchResult = repository.getMatchById(matchId)
            if (matchResult.isFailure) {
                _uiState.value = MatchDetailUiState(
                    isLoading = false,
                    error = matchResult.exceptionOrNull()?.message ?: "Hiba"
                )
                return@launch
            }

            val match = matchResult.getOrThrow()
            val extra = repository.getMatchExtraData(matchId)

            _uiState.value = MatchDetailUiState(
                isLoading = false,
                match = match,
                extra = extra
            )
        }
    }

    fun generateAiAnalysis() {
        val match = _uiState.value.match ?: return
        val extra = _uiState.value.extra

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true, selectedTab = 4)

            val analysis = buildString {
                appendLine("⚽ ${match.homeTeamName} vs ${match.awayTeamName}")
                appendLine("Liga: ${match.leagueName}")
                appendLine("Állás: ${match.score}")
                appendLine()
                appendLine("📊 Elemzés:")
                if (match.isLive) {
                    appendLine("• A meccs jelenleg élőben zajlik (${match.minute ?: "?"}').")
                } else if (match.status.contains("Finished", ignoreCase = true)) {
                    appendLine("• A mérkőzés véget ért.")
                } else {
                    appendLine("• A meccs még nem kezdődött el.")
                }

                if (extra.statistics.size >= 2) {
                    val home = extra.statistics[0]
                    val away = extra.statistics[1]
                    appendLine()
                    appendLine("Statisztikai összevetés:")
                    home.stats.forEach { (key, homeVal) ->
                        val awayVal = away.stats[key] ?: "-"
                        appendLine("• $key: $homeVal – $awayVal")
                    }
                } else {
                    appendLine()
                    appendLine("• Részletes statisztika még nem elérhető.")
                }

                if (extra.lineups.isNotEmpty()) {
                    appendLine()
                    appendLine("Felállások elérhetők (${extra.lineups.size} csapat).")
                }

                if (extra.highlights.isNotEmpty()) {
                    appendLine("Highlight videók: ${extra.highlights.size} db")
                }

                appendLine()
                appendLine("💡 Tipp: Figyeld a labdabirtoklást és a lövések számát – ezek gyakran előrejelzik a gólokat.")
            }

            _uiState.value = _uiState.value.copy(
                isAiLoading = false,
                aiAnalysis = analysis
            )
        }
    }
}
