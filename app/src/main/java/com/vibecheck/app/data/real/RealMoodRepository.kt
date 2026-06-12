package com.vibecheck.app.data.real

import com.google.firebase.firestore.FirebaseFirestore
import com.vibecheck.app.core.AppConfig
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.MoodCheckIn
import com.vibecheck.app.data.HeatmapRepository
import com.vibecheck.app.data.MoodRepository
import com.vibecheck.app.data.local.MoodCheckInEntity
import com.vibecheck.app.data.local.VibeCheckDatabase
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Room is the source of truth; each check-in also pushes one anonymised doc
 * to Firestore. Per the SOW, the doc id is a hash of timestamp + random salt
 * and the doc carries no uid — anonymous even to admins.
 */
class RealMoodRepository(
    private val db: VibeCheckDatabase,
    private val heatmapRepository: HeatmapRepository,
    private val firestore: FirebaseFirestore,
    private val appScope: CoroutineScope,
) : MoodRepository {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val todayCheckIn: Flow<MoodCheckIn?> =
        flow { emit(Unit) }.flatMapLatest {
            val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = start + 24L * 60 * 60 * 1000 - 1
            db.moodDao().observeBetween(start, end).map { it?.toModel() }
        }

    override fun history(): Flow<List<MoodCheckIn>> {
        val since = System.currentTimeMillis() -
            AppConfig.HISTORY_DAYS.toLong() * 24 * 60 * 60 * 1000
        return db.moodDao().observeSince(since).map { list -> list.map { it.toModel() } }
    }

    override suspend fun submitCheckIn(mood: Mood, note: String?): Result<MoodCheckIn> =
        runCatching {
            val trimmed = note?.trim()?.takeIf { it.isNotBlank() }
            val words = trimmed?.split(Regex("\\s+"))?.size ?: 0
            require(words <= AppConfig.MAX_NOTE_WORDS) {
                "Keep the note to ${AppConfig.MAX_NOTE_WORDS} words or fewer."
            }

            val now = System.currentTimeMillis()
            val regionId = heatmapRepository.resolveMyRegion().getOrNull()?.regionId
            val checkIn = MoodCheckIn(
                id = anonymousId(now),
                mood = mood,
                note = trimmed,
                timestampMillis = now,
                regionId = regionId,
            )

            db.moodDao().insert(MoodCheckInEntity.fromModel(checkIn))

            // Anonymised aggregate doc — fire and forget so offline check-ins work.
            appScope.launch {
                runCatching {
                    firestore.collection("checkins").document(checkIn.id).set(
                        mapOf(
                            "regionId" to regionId,
                            "mood" to mood.name,
                            "valence" to mood.valence,
                            "timestamp" to now,
                            // Deliberately no uid / IP / device id (SOW).
                        )
                    ).await()
                }
            }

            checkIn
        }

    /** SHA-256(timestamp + random salt) — unlinkable to the user. */
    private fun anonymousId(timestampMillis: Long): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(timestampMillis.toString().toByteArray() + salt)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
