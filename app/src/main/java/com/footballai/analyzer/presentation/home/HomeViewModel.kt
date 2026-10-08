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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val matchesByDate: Map<String, List<Match>> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val dayFormatter = DateTimeFormatter.ofPattern("yyyy. MMMM d., EEEE", Locale("hu", "HU"))

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.getTodayMatches()

            if (result.isSuccess) {
                val matches = result.getOrDefault(emptyList())
                val grouped = matches
                    .sortedBy { it.date }
                    .groupBy { match -> formatDateHeader(match.date) }
                _uiState.value = HomeUiState(
                    isLoading = false,
                    matchesByDate = grouped
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Ismeretlen hiba"
                )
            }
        }
    }

    private fun formatDateHeader(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Ismeretlen dátum"
        return try {
            val odt = OffsetDateTime.parse(dateString)
            val localDate = odt.toLocalDate()
            when (localDate) {
                LocalDate.now() -> "Ma – ${localDate.format(dayFormatter)}"
                LocalDate.now().plusDays(1) -> "Holnap – ${localDate.format(dayFormatter)}"
                LocalDate.now().minusDays(1) -> "Tegnap – ${localDate.format(dayFormatter)}"
                else -> localDate.format(dayFormatter)
            }
        } catch (e: Exception) {
            dateString.take(10)
        }
    }
}
