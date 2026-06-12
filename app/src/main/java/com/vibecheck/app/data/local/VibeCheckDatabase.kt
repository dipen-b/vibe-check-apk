package com.vibecheck.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.MoodCheckIn
import kotlinx.coroutines.flow.Flow

/**
 * Local source of truth for the user's own check-ins. Only their device
 * holds the full history; the server ever sees one anonymised doc per
 * check-in with no link back to this table.
 */
@Entity(tableName = "checkins")
data class MoodCheckInEntity(
    @PrimaryKey val id: String,
    val mood: String,
    val note: String?,
    val timestampMillis: Long,
    val regionId: String?,
) {
    fun toModel() = MoodCheckIn(
        id = id,
        mood = runCatching { Mood.valueOf(mood) }.getOrDefault(Mood.NEUTRAL),
        note = note,
        timestampMillis = timestampMillis,
        regionId = regionId,
    )

    companion object {
        fun fromModel(m: MoodCheckIn) = MoodCheckInEntity(
            id = m.id,
            mood = m.mood.name,
            note = m.note,
            timestampMillis = m.timestampMillis,
            regionId = m.regionId,
        )
    }
}

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MoodCheckInEntity)

    @Query("SELECT * FROM checkins WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
    fun observeSince(sinceMillis: Long): Flow<List<MoodCheckInEntity>>

    @Query("SELECT * FROM checkins WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis DESC LIMIT 1")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<MoodCheckInEntity?>

    @Query("DELETE FROM checkins")
    suspend fun deleteAll()
}

@Database(entities = [MoodCheckInEntity::class], version = 1, exportSchema = false)
abstract class VibeCheckDatabase : RoomDatabase() {
    abstract fun moodDao(): MoodDao

    companion object {
        @Volatile private var instance: VibeCheckDatabase? = null

        fun get(context: Context): VibeCheckDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VibeCheckDatabase::class.java,
                    "vibecheck.db",
                ).build().also { instance = it }
            }
    }
}
