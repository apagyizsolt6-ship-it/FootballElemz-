package com.footballai.analyzer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.footballai.analyzer.presentation.home.HomeScreen
import com.footballai.analyzer.presentation.matchdetail.MatchDetailScreen
import com.footballai.analyzer.presentation.settings.SettingsScreen
import com.footballai.analyzer.presentation.standings.StandingsScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object MatchDetail : Screen("match/{matchId}") {
        fun createRoute(matchId: Long) = "match/$matchId"
    }
    data object Standings : Screen("standings/{leagueId}/{season}/{title}/{homeId}/{awayId}") {
        fun createRoute(
            leagueId: Long,
            season: Int,
            title: String,
            homeId: Long = 0L,
            awayId: Long = 0L
        ): String {
            val enc = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "standings/$leagueId/$season/$enc/$homeId/$awayId"
        }
    }
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onMatchClick = { matchId ->
                    navController.navigate(Screen.MatchDetail.createRoute(matchId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onStandingsClick = { leagueId, season, title ->
                    if (leagueId > 0 && season > 0) {
                        navController.navigate(
                            Screen.Standings.createRoute(leagueId, season, title)
                        )
                    }
                }
            )
        }

        composable(
            route = Screen.MatchDetail.route,
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: return@composable
            MatchDetailScreen(
                matchId = matchId,
                onBack = { navController.popBackStack() },
                onStandingsClick = { leagueId, season, title, homeId, awayId ->
                    if (leagueId > 0 && season > 0) {
                        navController.navigate(
                            Screen.Standings.createRoute(
                                leagueId, season, title, homeId, awayId
                            )
                        )
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Standings.route,
            arguments = listOf(
                navArgument("leagueId") { type = NavType.LongType },
                navArgument("season") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType },
                navArgument("homeId") { type = NavType.LongType },
                navArgument("awayId") { type = NavType.LongType }
            )
        ) {
            StandingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
