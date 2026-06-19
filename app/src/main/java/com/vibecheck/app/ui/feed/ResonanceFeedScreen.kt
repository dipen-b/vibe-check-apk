package com.vibecheck.app.ui.feed

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vibecheck.app.core.Cities
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.ResonancePost
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.data.ResonanceScope
import com.vibecheck.app.ui.components.pressBounce
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceFeedScreen(container: AppContainer, onOpenSubscription: () -> Unit) {
    val isSubscribed by container.billingRepository.isSubscribed
        .collectAsStateWithLifecycle(initialValue = false)
    val todayCheckIn by container.moodRepository.todayCheckIn
        .collectAsStateWithLifecycle(initialValue = null)

    var myRegion by remember { mutableStateOf("us-nyc") } // Will be fetched on screen entry
    var scope by remember { mutableStateOf(ResonanceScope.MY_CITY) }
    var posts by remember { mutableStateOf<List<ResonancePost>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var showPostComposer by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope_obj = rememberCoroutineScope()

    // Fetch user's location on screen entry
    LaunchedEffect(Unit) {
        scope_obj.launch {
            container.heatmapRepository.resolveMyRegion().onSuccess { regionInfo ->
                myRegion = regionInfo.regionId
            }
        }
    }

    // Gate Global scope
    val canUseGlobal = isSubscribed
    val effectiveScope = if (scope == ResonanceScope.GLOBAL && !canUseGlobal) {
        ResonanceScope.MY_CITY
    } else {
        scope
    }

    LaunchedEffect(effectiveScope, myRegion) {
        loading = posts == null
        error = null
        container.resonanceRepository.feed(myRegion, effectiveScope, limit = 50).fold(
            onSuccess = { posts = it },
            onFailure = { error = it.message },
        )
        loading = false
        refreshing = false
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Resonance Feed",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Feel what others feel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Scope toggle
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (canUseGlobal) "Scope" else "My City (upgrade for Global)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${if (effectiveScope == ResonanceScope.MY_CITY) "My City" else "Global"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (canUseGlobal) {
                        Switch(
                            checked = scope == ResonanceScope.GLOBAL,
                            onCheckedChange = {
                                scope = if (it) ResonanceScope.GLOBAL else ResonanceScope.MY_CITY
                            },
                        )
                    }
                }

                if (!canUseGlobal && scope == ResonanceScope.GLOBAL) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenSubscription,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Unlock Global Feed")
                    }
                }
            }

            // Feed content
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope_obj.launch {
                        container.resonanceRepository.feed(myRegion, effectiveScope, limit = 50)
                            .fold(
                                onSuccess = { posts = it },
                                onFailure = { error = it.message },
                            )
                        refreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    error != null -> Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, textAlign = TextAlign.Center)
                    }
                    posts.isNullOrEmpty() -> Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("💬", fontSize = 60.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No vibes yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Be the first to share how you're feeling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 12.dp,
                        ),
                    ) {
                        items(posts!!) { post ->
                            ResonanceCard(post) { postId ->
                                scope_obj.launch {
                                    container.resonanceRepository.resonate(postId).onSuccess {
                                        // Optimistic update
                                        val idx = posts!!.indexOfFirst { it.id == postId }
                                        if (idx >= 0) {
                                            posts = posts!!.toMutableList().apply {
                                                this[idx] = this[idx].copy(
                                                    resonateCount = this[idx].resonateCount + 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            snackbar = { Snackbar(it) },
        )

        // Compose button (only if checked in today)
        if (todayCheckIn != null && !showPostComposer) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Violet)
                    .clickable { showPostComposer = true }
                    .padding(16.dp),
            ) {
                Text("✨", fontSize = 24.sp)
            }
        }
    }

    if (showPostComposer && todayCheckIn != null) {
        PostComposerDialog(
            userMood = todayCheckIn!!.mood,
            onClose = { showPostComposer = false },
            onSubmit = { text, imageUri ->
                scope_obj.launch {
                    container.resonanceRepository.submitPost(todayCheckIn!!.mood, text, myRegion, imageUri)
                        .fold(
                            onSuccess = {
                                posts = listOf(it) + (posts ?: emptyList())
                                showPostComposer = false
                                snackbar.showSnackbar("Posted! 💜")
                            },
                            onFailure = { snackbar.showSnackbar(it.message ?: "Error posting.") },
                        )
                }
            },
        )
    }
}

@Composable
private fun ResonanceCard(post: ResonancePost, onResonate: (String) -> Unit) {
    var isResonating by remember { mutableStateOf(false) }
    val resonateScale by animateFloatAsState(if (isResonating) 1.1f else 1f, label = "resonate")
    val city = Cities.byId(post.regionId)?.name ?: "Unknown"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(post.mood.emoji, fontSize = 24.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        post.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        city,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Image display
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Resonated by ${post.resonateCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(
                    onClick = {
                        isResonating = true
                        onResonate(post.id)
                    },
                    enabled = !isResonating,
                    modifier = Modifier
                        .height(36.dp)
                        .pressBounce(),
                ) {
                    Text("🔥 Resonate", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PostComposerDialog(userMood: Mood, onClose: () -> Unit, onSubmit: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val wordCount = remember(text) {
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }
    val charCount = text.length
    val isValid = wordCount in 1..5 && charCount <= 100

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Share your vibe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(userMood.emoji, fontSize = 32.sp)
                        Column {
                            Text(
                                userMood.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "1–5 words",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "$wordCount words",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wordCount > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 100) text = it },
                    placeholder = { Text("What's on your mind?") },
                    supportingText = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                if (wordCount > 5) "⚠️ Too many words (max 5)" else if (wordCount == 0) "Start typing..." else "✓ $wordCount word${if (wordCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wordCount > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "$charCount / 100",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (charCount > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    singleLine = true,
                    isError = wordCount > 5 || charCount > 100,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                // Image preview
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Remove image",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Gallery button
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("📷 Add photo (optional)")
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSubmit(text, selectedImageUri?.toString()) },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Post")
                    }
                }
            }
        }
    }
}
