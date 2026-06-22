package com.vibecheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.ui.navigation.AppNavHost
import com.vibecheck.app.ui.theme.VibeCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VibeCheckApp).container

        // Provide Activity to FriendshipRepository for phone auth
        (container.friendshipRepository as? com.vibecheck.app.data.real.RealFriendshipRepository)?.setCurrentActivity(this)

        setContent {
            val darkModePref by container.profileRepository.darkMode
                .collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val useDark = darkModePref ?: systemDark
            VibeCheckTheme(darkTheme = useDark) {
                AppNavHost(container)
            }
        }
    }
}
