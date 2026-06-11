package com.vibecheck.app.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vibecheck.app.data.AppContainer

@Composable
fun ChatScreen(container: AppContainer, sessionId: String, onClosed: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Chat — under construction")
    }
}
