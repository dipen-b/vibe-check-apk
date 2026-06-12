package com.vibecheck.app.ui.navigation

import com.vibecheck.app.core.model.Mood

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ACTIONS = "actions/{moodName}"
    const val CHAT = "chat/{sessionId}"
    const val SUBSCRIPTION = "subscription"

    fun actions(mood: Mood) = "actions/${mood.name}"
    fun chat(sessionId: String) = "chat/$sessionId"
}
