package com.vibecheck.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.core.model.ChatMessage
import com.vibecheck.app.core.model.ChatSession
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(container: AppContainer, sessionId: String, onClosed: () -> Unit) {
    val session by container.chatRepository.sessionState(sessionId)
        .collectAsStateWithLifecycle(initialValue = null)
    val messages by container.chatRepository.messages(sessionId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var secondsLeft by remember { mutableLongStateOf(300L) }
    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        while (true) {
            val remaining = (s.expiresAtMillis - System.currentTimeMillis()) / 1000
            secondsLeft = remaining.coerceAtLeast(0)
            if (remaining <= 0 || s.closed) { onClosed(); break }
            delay(1_000)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val sendMessage = {
        val text = messageText.trim()
        if (text.isNotEmpty() && !sending) {
            sending = true
            val snapshot = text
            messageText = ""
            scope.launch {
                container.chatRepository.sendMessage(sessionId, snapshot)
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Couldn't send.") }
                sending = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Anonymous chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        session?.peerMood?.let {
                            Text(
                                "${it.emoji} They're feeling ${it.label.lowercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Flag, contentDescription = "Report")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState, snackbar = { Snackbar(it) })
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            TimerBar(secondsLeft = secondsLeft, total = 300L)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { if (it.length <= 280) messageText = it },
                    placeholder = { Text("Say something…", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                IconButton(
                    onClick = sendMessage,
                    enabled = messageText.isNotBlank() && !sending,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (messageText.isNotBlank() && !sending) Violet else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ),
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report this chat?") },
            text = { Text("We'll flag the session and end the chat immediately. Thank you for keeping the space safe.") },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    scope.launch {
                        container.chatRepository.reportPeer(sessionId, "user_report")
                        container.chatRepository.leaveSession(sessionId)
                        onClosed()
                    }
                }) { Text("Report & leave", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TimerBar(secondsLeft: Long, total: Long) {
    val progress = secondsLeft.toFloat() / total.toFloat()
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60
    val isLow = secondsLeft <= 60

    val progressColor by animateColorAsState(
        targetValue = if (isLow) MaterialTheme.colorScheme.error else Violet,
        label = "timerColor",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Chat ends in",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "%d:%02d".format(mins, secs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ) + fadeIn(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(
                        elevation = if (message.fromMe) 3.dp else 1.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = true,
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.fromMe) {
                        Violet
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        message.text,
                        color = if (message.fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatTime(message.sentAtMillis),
                        color = if (message.fromMe) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}

