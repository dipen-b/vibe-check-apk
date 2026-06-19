package com.vibecheck.app.ui.friendship

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vibecheck.app.core.model.FriendRequest
import com.vibecheck.app.core.model.User
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.launch

@Composable
fun FriendsListScreen(container: AppContainer) {
    var tabState by remember { mutableStateOf(FriendsTab.MY_FRIENDS) }
    var friendsList by remember { mutableStateOf<List<User>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val friendsFlow = container.friendshipRepository.getFriendsList()
        .collectAsStateWithLifecycle(emptyList())
    val pendingFlow = container.friendshipRepository.getPendingRequests()
        .collectAsStateWithLifecycle(emptyList())
    val sentFlow = container.friendshipRepository.getSentRequests()
        .collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(friendsFlow.value) {
        friendsList = friendsFlow.value
    }

    LaunchedEffect(pendingFlow.value) {
        pendingRequests = pendingFlow.value
    }

    LaunchedEffect(sentFlow.value) {
        sentRequests = sentFlow.value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(Violet.copy(alpha = 0.1f))
                .padding(20.dp),
        ) {
            Column {
                Text(
                    "👥 Friends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Violet,
                )
                Text(
                    "Connect and chat with friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Tab Selector
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FriendsTab.values().forEach { tab ->
                OutlinedButton(
                    onClick = { tabState = tab },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                ) {
                    Text(
                        when (tab) {
                            FriendsTab.MY_FRIENDS -> "Friends (${friendsList.size})"
                            FriendsTab.REQUESTS -> "Requests (${pendingRequests.size})"
                            FriendsTab.SENT -> "Sent (${sentRequests.size})"
                        },
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Add Friend Button
        Button(
            onClick = { showAddFriendDialog = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Friend")
            Spacer(Modifier.size(4.dp))
            Text("Add Friend")
        }

        // Content
        when (tabState) {
            FriendsTab.MY_FRIENDS -> {
                if (friendsList.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No friends yet. Add one to get started!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(friendsList.size) { idx ->
                            FriendCard(friend = friendsList[idx])
                        }
                    }
                }
            }

            FriendsTab.REQUESTS -> {
                if (pendingRequests.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No pending requests",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(pendingRequests.size) { idx ->
                            FriendRequestCard(
                                request = pendingRequests[idx],
                                onAccept = {
                                    scope.launch {
                                        container.friendshipRepository.acceptFriendRequest(pendingRequests[idx].requestId)
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        container.friendshipRepository.rejectFriendRequest(pendingRequests[idx].requestId)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            FriendsTab.SENT -> {
                if (sentRequests.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No sent requests",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(sentRequests.size) { idx ->
                            SentRequestCard(request = sentRequests[idx])
                        }
                    }
                }
            }
        }
    }

    // Add Friend Dialog
    if (showAddFriendDialog) {
        Dialog(onDismissRequest = { showAddFriendDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Text(
                        "Search & Add Friend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isNotEmpty()) {
                                loading = true
                                scope.launch {
                                    container.friendshipRepository.searchUsers(it)
                                        .onSuccess { results ->
                                            searchResults = results
                                            loading = false
                                        }
                                        .onFailure {
                                            loading = false
                                        }
                                }
                            } else {
                                searchResults = emptyList()
                            }
                        },
                        placeholder = { Text("Search by name or phone...") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(12.dp))

                    if (loading) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(searchResults.size) { idx ->
                                val user = searchResults[idx]
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${user.firstName} ${user.lastName}",
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            user.countryCode,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                container.friendshipRepository.sendFriendRequest(user.userId)
                                                    .onSuccess {
                                                        searchQuery = ""
                                                        searchResults = emptyList()
                                                    }
                                            }
                                        },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Text("+")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showAddFriendDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

enum class FriendsTab {
    MY_FRIENDS,
    REQUESTS,
    SENT,
}

@Composable
private fun FriendCard(friend: User) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (friend.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Violet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(friend.firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    "${friend.firstName} ${friend.lastName}",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Joined ${friend.createdAtMillis}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Violet.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (request.senderAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = request.senderAvatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Violet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(request.senderFirstName.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "${request.senderFirstName} ${request.senderLastName}",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Wants to be your friend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun SentRequestCard(request: FriendRequest) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Friend request sent to ${request.receiverId}",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Waiting for response",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("⏳", fontSize = 24.sp)
        }
    }
}
