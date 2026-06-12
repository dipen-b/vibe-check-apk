package com.vibecheck.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.chat.ChatScreen
import com.vibecheck.app.ui.home.HomeScaffold
import com.vibecheck.app.ui.onboarding.OnboardingScreen
import com.vibecheck.app.ui.actions.ActionsScreen
import com.vibecheck.app.ui.splash.SplashScreen
import com.vibecheck.app.ui.subscription.SubscriptionScreen

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val profileState by container.profileRepository.profileState
        .collectAsStateWithLifecycle(initialValue = ProfileState.Loading)

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                profileState = profileState,
                onNavigate = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(container) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }
        }
        composable(Routes.HOME) {
            HomeScaffold(
                container = container,
                onCheckedIn = { mood -> navController.navigate(Routes.actions(mood)) },
                onChatStarted = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
            )
        }
        composable(
            route = Routes.ACTIONS,
            arguments = listOf(navArgument("moodName") { type = NavType.StringType }),
        ) { backStackEntry ->
            val moodName = backStackEntry.arguments?.getString("moodName") ?: Mood.NEUTRAL.name
            val mood = runCatching { Mood.valueOf(moodName) }.getOrDefault(Mood.NEUTRAL)
            ActionsScreen(container, mood) { navController.popBackStack() }
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            ChatScreen(container, sessionId) { navController.popBackStack() }
        }
        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(container) { navController.popBackStack() }
        }
    }
}
