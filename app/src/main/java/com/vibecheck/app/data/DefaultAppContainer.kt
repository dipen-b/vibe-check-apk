package com.vibecheck.app.data

import android.app.Application
import com.vibecheck.app.billing.PlayBillingRepository
import com.vibecheck.app.data.fake.FakeChatRepository
import com.vibecheck.app.data.fake.FakeInsightsRepository
import com.vibecheck.app.data.fake.FakeMicroActionEngine
import com.vibecheck.app.data.firebase.FirebaseProvider
import com.vibecheck.app.data.local.ProfilePreferences
import com.vibecheck.app.data.local.VibeCheckDatabase
import com.vibecheck.app.data.real.RealHeatmapRepository
import com.vibecheck.app.data.real.RealMoodRepository
import com.vibecheck.app.data.real.RealProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Production container: Room + DataStore + Firebase for the core flow
 * (profile, mood, heatmap).
 *
 * NOTE module split (2026-06): chat, billing and insights belong to the
 * social/account module and still use the fake implementations here —
 * that module's owner swaps them for real ones on their branch.
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

    override val moodRepository = RealMoodRepository(
        db = db,
        heatmapRepository = heatmapRepository,
        firestore = FirebaseProvider.firestore,
        appScope = appScope,
    )

    // The engine is rule-based and fully on-device; the "fake" catalogue
    // is the real catalogue.
    override val microActionEngine = FakeMicroActionEngine()

    // ---- Social & account module (other owner) ----
    override val chatRepository = FakeChatRepository(appScope)
    // Billing module owner: real Play Billing now backs production.
    override val billingRepository = PlayBillingRepository(app, appScope)
    override val insightsRepository = FakeInsightsRepository(moodRepository, billingRepository)
}
