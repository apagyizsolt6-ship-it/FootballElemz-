package com.footballai.analyzer.presentation.settings

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballai.analyzer.ui.theme.DarkBackground
import com.footballai.analyzer.ui.theme.DarkCard
import com.footballai.analyzer.ui.theme.GreenPrimary
import com.footballai.analyzer.ui.theme.TextPrimary
import com.footballai.analyzer.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Beállítások",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Vissza",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ===== Highlightly =====
            Text(
                text = "Highlightly API kulcs",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Meccsek, statisztikák, felállások adatai. Csak a telefonodon tárolódik.",
                color = TextSecondary,
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
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ===== LLM =====
            Text(
                text = "LLM API (AI elemzés)",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "OpenAI, xAI Grok vagy más OpenAI-kompatibilis szolgáltató.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "API kulcs", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.llmKey,
                onValueChange = viewModel::onLlmKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-... vagy xai-...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Base URL", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.llmBaseUrl,
                onValueChange = viewModel::onLlmBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.openai.com/v1/") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Modell", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.llmModel,
                onValueChange = viewModel::onLlmModelChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("gpt-4o-mini / grok-2") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Példák:\n• OpenAI → https://api.openai.com/v1/ + gpt-4o-mini\n• xAI Grok → https://api.x.ai/v1/ + grok-2",
                color = TextSecondary,
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
                    containerColor = GreenPrimary,
                    contentColor = DarkBackground
                )
            ) {
                Text("Mentés", fontWeight = FontWeight.Bold)
            }

            if (uiState.message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.message ?: "",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    unfocusedBorderColor = DarkCard,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = GreenPrimary,
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary
)
