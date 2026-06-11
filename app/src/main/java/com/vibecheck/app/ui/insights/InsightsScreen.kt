package com.vibecheck.app.ui.insights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vibecheck.app.data.AppContainer

@Composable
fun InsightsScreen(container: AppContainer, onUpgrade: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Insights — under construction")
    }
}
