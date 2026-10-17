package com.footballai.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.footballai.analyzer.presentation.ThemeViewModel
import com.footballai.analyzer.presentation.home.HomeScreen
import com.footballai.analyzer.presentation.matchdetail.MatchDetailScreen
import com.footballai.analyzer.presentation.settings.SettingsScreen
import com.footballai.analyzer.ui.theme.FociElemzoTheme
import com.footballai.analyzer.ui.theme.LocalAppColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeVm: ThemeViewModel = hiltViewModel()
            val isDark by themeVm.isDarkTheme.collectAsState()

            FociElemzoTheme(darkTheme = isDark) {
                val colors = LocalAppColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onMatchClick = { matchId ->
                                    navController.navigate("match/$matchId")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable(
                            route = "match/{matchId}",
                            arguments = listOf(
                                navArgument("matchId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
                            MatchDetailScreen(
                                matchId = matchId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
