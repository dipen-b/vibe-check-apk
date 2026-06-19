package com.vibecheck.app.data

import android.app.Application
import com.vibecheck.app.billing.PlayBillingRepository
import com.vibecheck.app.data.fake.FakeInsightsRepository
import com.vibecheck.app.data.fake.FakeMicroActionEngine
import com.vibecheck.app.data.firebase.FirebaseProvider
import com.vibecheck.app.data.local.ProfilePreferences
import com.vibecheck.app.data.local.VibeCheckDatabase
import com.vibecheck.app.data.real.RealChatRepository
import com.vibecheck.app.data.real.RealFriendshipRepository
import com.vibecheck.app.data.real.RealHeatmapRepository
import com.vibecheck.app.data.real.RealMoodRepository
import com.vibecheck.app.data.real.RealProfileRepository
import com.vibecheck.app.data.real.RealResonanceRepository
import com.vibecheck.app.data.real.RealQuestRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Production container: Room + DataStore + Firebase.
 *
 * Core flow (profile, mood, heatmap) and anonymous chat are real. Billing is
 * real Play Billing (billing module). Insights still uses the fake (insights
 * module owner swaps it on their branch); the micro-action engine is
 * rule-based and fully on-device, so its "fake" catalogue is the real one.
 */
class DefaultAppContainer(app: Application) : AppContainer {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = VibeCheckDatabase.get(app)
    private val prefs = ProfilePreferences(app)

    override val profileRepository = RealProfileRepository(
        prefs = prefs,
        db = db,
        auth = FirebaseProvider.auth,
        firestore = FirebaseProvider.firestore,
    )

    override val heatmapRepository = RealHeatmapRepository(
        context = app,
        firestore = FirebaseProvider.firestore,
    )

    override val resonanceRepository = RealResonanceRepository(
        firestore = FirebaseProvider.firestore,
        auth = FirebaseProvider.auth,
        storage = FirebaseStorage.getInstance(),
        context = app,
    )

    override val questRepository = RealQuestRepository(
        firestore = FirebaseProvider.firestore,
        auth = FirebaseProvider.auth,
    )

    override val moodRepository = RealMoodRepository(
        db = db,
        heatmapRepository = heatmapRepository,
        firestore = FirebaseProvider.firestore,
        appScope = appScope,
    )

    override val microActionEngine = FakeMicroActionEngine()

    // Real Play Billing backs production (billing module).
    override val billingRepository = PlayBillingRepository(app, appScope)

    // Anonymous chat: Firestore + callable Cloud Functions, profanity-filtered.
    override val chatRepository = RealChatRepository(
        auth = FirebaseProvider.auth,
        firestore = FirebaseProvider.firestore,
        functions = FirebaseProvider.functions,
        moodRepository = moodRepository,
        prefs = prefs,
        billingRepository = billingRepository,
    )

    // Insights module owner swaps this for a real implementation on their branch.
    override val insightsRepository = FakeInsightsRepository(moodRepository, billingRepository)

    override val friendshipRepository = RealFriendshipRepository(
        context = app,
        auth = FirebaseProvider.auth,
        firestore = FirebaseProvider.firestore,
        storage = FirebaseStorage.getInstance(),
    )
}
