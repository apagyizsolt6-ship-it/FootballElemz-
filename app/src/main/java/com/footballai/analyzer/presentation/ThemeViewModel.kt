package com.footballai.analyzer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballai.analyzer.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = userPreferences.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun toggleTheme() {
        viewModelScope.launch {
            userPreferences.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkTheme(dark)
        }
    }
}
