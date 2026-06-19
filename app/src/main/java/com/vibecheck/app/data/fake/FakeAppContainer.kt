package com.vibecheck.app.data.fake

import android.app.Activity
import android.app.Application
import com.vibecheck.app.core.AppConfig
import com.vibecheck.app.core.Cities
import com.vibecheck.app.core.model.ActionCategory
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.model.ChatMessage
import com.vibecheck.app.core.model.ChatSession
import com.vibecheck.app.core.model.HeatmapScope
import com.vibecheck.app.core.model.MatchState
import com.vibecheck.app.core.model.MicroAction
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.MoodCheckIn
import com.vibecheck.app.core.model.MoodTrendPoint
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.core.model.RegionInfo
import com.vibecheck.app.core.model.RegionMoodAggregate
import com.vibecheck.app.core.model.UserProfile
import com.vibecheck.app.core.model.WeeklyInsights
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.data.BillingRepository
import com.vibecheck.app.data.ChatRepository
import com.vibecheck.app.data.HeatmapRepository
import com.vibecheck.app.data.InsightsRepository
import com.vibecheck.app.data.MicroActionEngine
import com.vibecheck.app.data.MoodRepository
import com.vibecheck.app.data.ProfileRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * In-memory implementation of every repository. Used when
 * BuildConfig.USE_FAKE_DATA is true (debug default) so the full product is
 * demoable with rich data and zero backend. Also reused by unit tests.
 */
class FakeAppContainer(@Suppress("unused") private val app: Application) : AppContainer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val profileRepository = FakeProfileRepository()
    override val moodRepository = FakeMoodRepository()
    override val heatmapRepository = FakeHeatmapRepository()
    override val billingRepository = FakeBillingRepository()
    override val chatRepository = FakeChatRepository(scope, billingRepository)
    override val insightsRepository = FakeInsightsRepository(moodRepository, billingRepository)
    override val microActionEngine = FakeMicroActionEngine()
}

class FakeProfileRepository : ProfileRepository {
    private val state = MutableStateFlow<ProfileState>(ProfileState.NeedsOnboarding)
    override val profileState: Flow<ProfileState> = state

    override suspend fun completeOnboarding(ageBracket: AgeBracket, username: String?): Result<UserProfile> {
        if (ageBracket == AgeBracket.UNDER_16) {
            return Result.failure(IllegalArgumentException("VibeCheck is only for people aged ${AppConfig.MIN_AGE_YEARS}+."))
        }
        val profile = UserProfile(
            uid = "fake-${UUID.randomUUID()}",
            username = username?.trim()?.takeIf { it.isNotEmpty() },
            ageBracket = ageBracket,
            chatOptIn = false,
            createdAtMillis = System.currentTimeMillis(),
        )
        state.value = ProfileState.Ready(profile)
        return Result.success(profile)
    }

    override suspend fun setChatOptIn(optIn: Boolean) {
        val current = (state.value as? ProfileState.Ready)?.profile ?: return
        state.value = ProfileState.Ready(current.copy(chatOptIn = optIn))
    }

    override suspend fun setUsername(username: String?): Result<Unit> {
        val current = (state.value as? ProfileState.Ready)?.profile
            ?: return Result.failure(IllegalStateException("Not onboarded yet."))
        state.value = ProfileState.Ready(
            current.copy(username = username?.trim()?.takeIf { it.isNotEmpty() })
        )
        return Result.success(Unit)
    }

    override suspend fun deleteAllMyData(): Result<Unit> {
        state.value = ProfileState.NeedsOnboarding
        return Result.success(Unit)
    }
}

class FakeMoodRepository : MoodRepository {
    private val checkIns = MutableStateFlow(seedHistory())

    override val todayCheckIn: Flow<MoodCheckIn?> =
        checkIns.map { list -> list.firstOrNull { isToday(it.timestampMillis) } }

    override fun history(): Flow<List<MoodCheckIn>> = checkIns

    override suspend fun submitCheckIn(mood: Mood, note: String?): Result<MoodCheckIn> {
        val trimmed = note?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed != null && trimmed.split(Regex("\\s+")).size > AppConfig.MAX_NOTE_WORDS) {
            return Result.failure(IllegalArgumentException("Keep the note to ${AppConfig.MAX_NOTE_WORDS} words or fewer."))
        }
        if (checkIns.value.any { isToday(it.timestampMillis) }) {
            return Result.failure(IllegalStateException("You already checked in today — come back tomorrow."))
        }
        val checkIn = MoodCheckIn(
            id = UUID.randomUUID().toString(),
            mood = mood,
            note = trimmed,
            timestampMillis = System.currentTimeMillis(),
            regionId = "us-nyc",
        )
        checkIns.update { listOf(checkIn) + it }
        return Result.success(checkIn)
    }

    private fun isToday(millis: Long): Boolean {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() == LocalDate.now(zone)
    }

    private fun seedHistory(): List<MoodCheckIn> {
        val moods = listOf(
            Mood.HAPPY, Mood.NEUTRAL, Mood.TIRED, Mood.SAD, Mood.HAPPY, Mood.EXCITED, Mood.NEUTRAL,
            Mood.TIRED, Mood.HAPPY, Mood.NEUTRAL, Mood.SAD, Mood.HAPPY, Mood.HAPPY, Mood.TIRED,
        )
        val notes = listOf(
            "good coffee", null, "long day", "miss home", null, "friday!", null,
            "slept badly", "sunny walk", null, "rough meeting", "nice lunch", null, "late night",
        )
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        // Seeds start yesterday so today's check-in flow is demoable.
        return moods.mapIndexed { i, mood ->
            MoodCheckIn("seed-$i", mood, notes[i], now - (i + 1) * dayMs, "us-nyc")
        }
    }
}

class FakeHeatmapRepository : HeatmapRepository {
    private val myRegion = Cities.byId("us-nyc") ?: Cities.ALL.first()

    override suspend fun aggregates(scope: HeatmapScope): Result<List<RegionMoodAggregate>> {
        delay(400) // simulate network latency
        val regions = when (scope) {
            HeatmapScope.LOCAL -> Cities.ALL.filter {
                it.countryCode == myRegion.countryCode &&
                    Cities.distanceKm(it.latitude, it.longitude, myRegion.latitude, myRegion.longitude) < 400
            }
            HeatmapScope.NATIONAL -> Cities.ALL.filter { it.countryCode == myRegion.countryCode }
            HeatmapScope.GLOBAL -> Cities.ALL
        }
        return Result.success(regions.map { region ->
            val h = abs(region.regionId.hashCode())
            RegionMoodAggregate(
                region = region,
                checkInCount = 40 + h % 400,
                averageValence = 0.25f + (h / 7 % 60) / 100f,
            )
        })
    }

    override suspend fun resolveMyRegion(): Result<RegionInfo> = Result.success(myRegion)
}

class FakeChatRepository(private val scope: CoroutineScope, private val billingRepository: FakeBillingRepository) : ChatRepository {
    private val sessions = MutableStateFlow<Map<String, ChatSession>>(emptyMap())
    private val messageStore = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val cannedReplies = listOf(
        "yeah, i get that", "what helped you last time?", "same here honestly",
        "that sounds heavy", "small wins still count", "glad we matched 🙂",
    )
    private var replyIndex = 0
    private var autoCloseJob: Job? = null
    private val trialUsed = MutableStateFlow(false)

    override fun requestMatch(): Flow<MatchState> = flow {
        emit(MatchState.Searching)
        delay(2500)
        val now = System.currentTimeMillis()
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            startedAtMillis = now,
            expiresAtMillis = now + AppConfig.CHAT_DURATION_MINUTES * 60_000L,
            closed = false,
            peerMood = Mood.NEUTRAL,
        )
        sessions.update { it + (session.id to session) }
        messageStore.update {
            it + (session.id to listOf(
                ChatMessage(UUID.randomUUID().toString(), session.id, false, "hey, rough day here too. you ok?", now)
            ))
        }
        autoCloseJob = scope.launch {
            delay(session.expiresAtMillis - System.currentTimeMillis())
            closeSession(session.id)
        }
        emit(MatchState.Matched(session.id))
    }

    override suspend fun cancelMatch() {
        // The match flow is cold; cancelling the collector cancels the search.
    }

    override fun sessionState(sessionId: String): Flow<ChatSession?> =
        sessions.map { it[sessionId] }

    override fun messages(sessionId: String): Flow<List<ChatMessage>> =
        messageStore.map { it[sessionId].orEmpty() }

    override suspend fun sendMessage(sessionId: String, text: String): Result<Unit> {
        val session = sessions.value[sessionId]
            ?: return Result.failure(IllegalStateException("Chat not found."))
        if (session.closed || System.currentTimeMillis() > session.expiresAtMillis) {
            return Result.failure(IllegalStateException("This chat has ended."))
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Message is empty."))
        appendMessage(sessionId, fromMe = true, text = trimmed)
        scope.launch {
            delay(1500)
            val stillOpen = sessions.value[sessionId]?.closed == false
            if (stillOpen) {
                appendMessage(sessionId, fromMe = false, text = cannedReplies[replyIndex % cannedReplies.size])
                replyIndex++
            }
        }
        return Result.success(Unit)
    }

    override suspend fun reportPeer(sessionId: String, reason: String): Result<Unit> {
        closeSession(sessionId)
        return Result.success(Unit)
    }

    override suspend fun leaveSession(sessionId: String) {
        closeSession(sessionId)
    }

    override fun canAccessMatch(): Flow<Boolean> =
        kotlinx.coroutines.flow.combine(trialUsed, billingRepository.isSubscribed) { used, isPremium ->
            !used || isPremium
        }

    override suspend fun markTrialUsed() {
        trialUsed.value = true
    }

    override suspend fun getOpeningSuggestions(peerMood: Mood, userMood: Mood): Result<List<String>> =
        Result.success(listOf(
            "hey, how's your day going?",
            "i'm feeling similar vibes — mind chatting?",
            "we matched on mood! want to talk?",
        ))

    override suspend fun getReplySuggestions(sessionId: String, lastMessage: String): Result<List<String>> =
        Result.success(listOf(
            "totally get that",
            "yeah, me too",
            "that makes sense",
        ))

    override fun previousMatches(): kotlinx.coroutines.flow.Flow<List<com.vibecheck.app.core.model.PreviousMatch>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun savePreviousMatch(match: com.vibecheck.app.core.model.PreviousMatch) {
        // No-op in debug
    }

    override suspend fun requestReMatch(previousMatchSessionId: String): Result<String> =
        Result.success(java.util.UUID.randomUUID().toString()) // return fake new session ID

    private fun appendMessage(sessionId: String, fromMe: Boolean, text: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            fromMe = fromMe,
            text = text,
            sentAtMillis = System.currentTimeMillis(),
        )
        messageStore.update { it + (sessionId to (it[sessionId].orEmpty() + message)) }
    }

    private fun closeSession(sessionId: String) {
        sessions.update { map ->
            map[sessionId]?.let { map + (sessionId to it.copy(closed = true)) } ?: map
        }
    }
}

class FakeBillingRepository : BillingRepository {
    private val subscribed = MutableStateFlow(false)
    override val isSubscribed: Flow<Boolean> = subscribed
    // Dual currency per the SOW (US + UK). The real PlayBillingRepository reads
    // the localised price from Play; this is the demo stand-in.
    override val monthlyPriceFormatted: Flow<String?> = MutableStateFlow("$3.99 / £3.99")

    override suspend fun launchPurchase(activity: Activity): Result<Unit> {
        delay(1200) // simulate the Play purchase sheet (demo — no real charge)
        subscribed.value = true
        return Result.success(Unit)
    }

    /** Demo affordance: lets QA reset entitlement to re-test the paywall. */
    override suspend fun refresh() {
        // no-op in demo; real impl re-queries Play + server entitlement
    }
}

class FakeInsightsRepository(
    private val moodRepository: MoodRepository,
    private val billingRepository: BillingRepository,
) : InsightsRepository {

    override suspend fun weeklyInsights(): Result<WeeklyInsights> {
        val history = moodRepository.history().first()
        val premium = billingRepository.isSubscribed.first()
        val zone = ZoneId.systemDefault()
        val weekStart = LocalDate.now(zone).with(DayOfWeek.MONDAY)

        val points = (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            val dayCheckIns = history.filter {
                Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() == day
            }
            MoodTrendPoint(
                dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                averageValence = if (dayCheckIns.isEmpty()) null
                else dayCheckIns.map { it.mood.valence }.average().toFloat(),
                dominantMood = dayCheckIns.firstOrNull()?.mood,
            )
        }
        val withData = points.filter { it.averageValence != null }
        val best = withData.maxByOrNull { it.averageValence!! }?.dayLabel
        val worst = withData.minByOrNull { it.averageValence!! }?.dayLabel
        return Result.success(
            WeeklyInsights(
                weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                points = points,
                bestDay = best,
                toughestDay = worst,
                patternSummary = if (premium) {
                    "Your mood tends to dip on ${worst ?: "Tuesdays"} — often linked to low social interaction. " +
                        "Planning one small social touchpoint that day may help."
                } else {
                    "Subscribe to VibeCheck Plus to see why your mood moves the way it does."
                },
                premium = premium,
            )
        )
    }

    override suspend fun exportHistoryCsv(): Result<String> {
        if (!billingRepository.isSubscribed.first()) {
            return Result.failure(IllegalStateException("History export is a VibeCheck Plus feature."))
        }
        val history = moodRepository.history().first()
        val sb = StringBuilder("timestamp_iso,mood,valence,note\n")
        history.forEach { checkIn ->
            val iso = Instant.ofEpochMilli(checkIn.timestampMillis).toString()
            sb.append("$iso,${checkIn.mood.name},${checkIn.mood.valence},${csvField(checkIn.note.orEmpty())}\n")
        }
        return Result.success(sb.toString())
    }

    /**
     * CSV-quote a field and neutralise spreadsheet formula injection: a cell
     * starting with = + - @ (or tab/CR) is treated as a formula by Excel/Sheets,
     * so prefix those with a single quote. The note is the only user-controlled
     * field, but quoting everything is harmless.
     */
    private fun csvField(raw: String): String {
        val needsGuard = raw.isNotEmpty() && raw.first() in charArrayOf('=', '+', '-', '@', '\t', '\r')
        val guarded = if (needsGuard) "'$raw" else raw
        return "\"" + guarded.replace("\"", "\"\"") + "\""
    }
}

class FakeMicroActionEngine : MicroActionEngine {
    private val catalogue: Map<Mood, List<MicroAction>> = mapOf(
        Mood.HAPPY to listOf(
            MicroAction("happy-1", "Share the vibe", "Send someone a genuine compliment — it doubles the glow.", 2, ActionCategory.SOCIAL),
            MicroAction("happy-2", "Gratitude snapshot", "Note three things fuelling today's mood.", 2, ActionCategory.GRATITUDE),
            MicroAction("happy-3", "Ride the wave", "Knock out the small task you've been putting off.", 2, ActionCategory.CREATIVE),
        ),
        Mood.NEUTRAL to listOf(
            MicroAction("meh-1", "60-second reset", "Box breathing: in 4, hold 4, out 4, hold 4. Repeat 4 times.", 2, ActionCategory.BREATHING),
            MicroAction("meh-2", "Change the scenery", "Step outside or to a window for two minutes.", 2, ActionCategory.MOVEMENT),
            MicroAction("meh-3", "Tiny spark", "Play one song you loved as a teenager.", 2, ActionCategory.CREATIVE),
        ),
        Mood.SAD to listOf(
            MicroAction("sad-1", "4-7-8 breathing", "Inhale 4s, hold 7s, exhale 8s. Three slow rounds.", 2, ActionCategory.BREATHING),
            MicroAction("sad-2", "Reach out", "Message one person you trust — just \"hey, thinking of you\".", 2, ActionCategory.SOCIAL),
            MicroAction("sad-3", "Warmth break", "Make a warm drink and drink it away from screens.", 2, ActionCategory.REST),
        ),
        Mood.ANGRY to listOf(
            MicroAction("angry-1", "Shake it off", "30 seconds of fast movement — stairs, jumping jacks, a brisk walk.", 2, ActionCategory.MOVEMENT),
            MicroAction("angry-2", "Cold reset", "Splash cold water on your face and name the feeling out loud.", 2, ActionCategory.REST),
            MicroAction("angry-3", "Unsent letter", "Write what you'd like to say. Don't send it.", 2, ActionCategory.CREATIVE),
        ),
        Mood.TIRED to listOf(
            MicroAction("tired-1", "Power pause", "Two minutes, eyes closed, shoulders dropped, slow breaths.", 2, ActionCategory.REST),
            MicroAction("tired-2", "Stretch tall", "Stand, reach for the ceiling, roll your neck slowly.", 2, ActionCategory.MOVEMENT),
            MicroAction("tired-3", "Water first", "Drink a full glass of water before the next coffee.", 2, ActionCategory.REST),
        ),
        Mood.EXCITED to listOf(
            MicroAction("excited-1", "Channel it", "Jot the three ideas buzzing loudest before they fade.", 2, ActionCategory.CREATIVE),
            MicroAction("excited-2", "Spread it", "Tell someone what you're excited about.", 2, ActionCategory.SOCIAL),
            MicroAction("excited-3", "Anchor it", "One slow breath cycle so the energy lasts the day.", 2, ActionCategory.BREATHING),
        ),
    )

    override fun suggestFor(mood: Mood, recentHistory: List<MoodCheckIn>): MicroAction =
        catalogue.getValue(mood).first()

    override fun alternativesFor(mood: Mood): List<MicroAction> = catalogue.getValue(mood)
}
