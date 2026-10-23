package com.footballai.analyzer.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballai.analyzer.presentation.ThemeViewModel
import com.footballai.analyzer.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark by themeViewModel.isDarkTheme.collectAsState()
    val c = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Beállítások",
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Vissza",
                            tint = c.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background)
            )
        },
        containerColor = c.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Megjelenés",
                color = c.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDark) "Sötét mód" else "Világos mód",
                    color = c.textPrimary
                )
                Switch(
                    checked = isDark,
                    onCheckedChange = { themeViewModel.setDarkTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = c.green,
                        checkedThumbColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Highlightly API kulcs",
                color = c.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Meccsek, statisztikák, felállások. Csak a telefonodon tárolódik.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Highlightly API kulcs") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(c)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "LLM API (AI elemzés)",
                color = c.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "OpenAI, xAI Grok vagy más OpenAI-kompatibilis szolgáltató.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "API kulcs", color = c.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.llmKey,
                onValueChange = viewModel::onLlmKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-... vagy xai-...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(c)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Base URL", color = c.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.llmBaseUrl,
                onValueChange = viewModel::onLlmBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.x.ai/v1/") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(c)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Modell", color = c.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.llmModel,
                onValueChange = viewModel::onLlmModelChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("grok-4.7 / gpt-4o-mini") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(c)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Példák:\n• xAI Grok → https://api.x.ai/v1/ + grok-4.7\n• OpenAI → https://api.openai.com/v1/ + gpt-4o-mini",
                color = c.textSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.saveAll() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.green,
                    contentColor = if (c.isDark) c.background else Color.White
                )
            ) {
                Text("Mentés", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.clearCaches() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = c.surface)
            ) {
                Text("Cache törlése", color = c.textPrimary)
            }
            Text(
                "Törli a helyi meccs- és AI-gyorsítótárat. A következő betöltés API-t hív.",
                color = c.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )


            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Kedvenc csapatok",
                color = c.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "A listában csillaggal jelölt csapatok. A „Kedvencek” szűrő ezeket mutatja.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.favoriteTeams.isEmpty()) {
                Text("Még nincs kedvenc csapat.", color = c.textSecondary, fontSize = 13.sp)
            } else {
                uiState.favoriteTeams.forEach { team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(team, color = c.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            "Eltávolít",
                            color = c.redLive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { viewModel.removeFavorite(team) }
                        )
                    }
                }
            }

            if (uiState.message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.message ?: "",
                    color = c.green,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun fieldColors(c: com.footballai.analyzer.ui.theme.AppColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.green,
        unfocusedBorderColor = c.card,
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        cursorColor = c.green,
        focusedPlaceholderColor = c.textSecondary,
        unfocusedPlaceholderColor = c.textSecondary,
        focusedContainerColor = c.surface,
        unfocusedContainerColor = c.surface
    )
