package com.vibecheck.app.ui.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.data.AppContainer

@Composable
fun ActionsScreen(container: AppContainer, mood: Mood, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Micro-actions — under construction")
    }
}
