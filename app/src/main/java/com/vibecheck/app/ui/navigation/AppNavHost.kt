package com.vibecheck.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vibecheck.app.ui.subscription.SubscriptionScreen

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val profileState by container.profileRepository.profileState
        .collectAsStateWithLifecycle(initialValue = ProfileState.Loading)

    // Decide the start destination once, after the first non-loading state.
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(profileState) {
        if (startDestination == null && profileState !is ProfileState.Loading) {
            startDestination =
                if (profileState is ProfileState.Ready) Routes.HOME else Routes.ONBOARDING
        } else if (startDestination == Routes.HOME && profileState is ProfileState.NeedsOnboarding) {
            // "Delete all my data" wipes the profile mid-session.
            navController.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
        }
    }

    val start = startDestination
    if (start == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = start) {
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
