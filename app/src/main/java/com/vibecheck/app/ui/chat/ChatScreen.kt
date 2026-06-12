package com.vibecheck.app.ui.chat

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.core.model.ChatMessage
import com.vibecheck.app.core.model.ChatSession
import com.vibecheck.app.data.AppContainer
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

    // Live countdown based on session expiry
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

    // Auto-scroll to latest message
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
                        Text("Anonymous chat", fontWeight = FontWeight.Medium)
                        session?.peerMood?.let {
                            Text(
                                "They're feeling ${it.name.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
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
            // Timer bar
            TimerBar(secondsLeft = secondsLeft, total = 300L)

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
            }

            // Helplines footer
            HelplinesFooter()
            Spacer(Modifier.height(6.dp))

            // Input
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { if (it.length <= 280) messageText = it },
                    placeholder = { Text("Say something…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = sendMessage,
                    enabled = messageText.isNotBlank() && !sending,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Chat ends in",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "%d:%02d".format(mins, secs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val bubbleColor = if (msg.fromMe) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.fromMe) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (msg.fromMe) 18.dp else 4.dp,
                        bottomEnd = if (msg.fromMe) 4.dp else 18.dp,
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(msg.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
