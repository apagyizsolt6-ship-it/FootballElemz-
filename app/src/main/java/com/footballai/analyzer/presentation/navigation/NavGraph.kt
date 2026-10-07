package com.footballai.analyzer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.footballai.analyzer.presentation.home.HomeScreen
import com.footballai.analyzer.presentation.matchdetail.MatchDetailScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object MatchDetail : Screen("match/{matchId}") {
        fun createRoute(matchId: Long) = "match/$matchId"
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
                }
            )
        }

        composable(
            route = Screen.MatchDetail.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: return@composable
            MatchDetailScreen(
                matchId = matchId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
