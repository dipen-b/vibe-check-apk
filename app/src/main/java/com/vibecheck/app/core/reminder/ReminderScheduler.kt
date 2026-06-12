package com.vibecheck.app.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vibecheck.app.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_prefs")
private val ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
private const val WORK_NAME = "daily_mood_reminder"
private const val CHANNEL_ID = "daily_reminder"

object ReminderScheduler {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enable(context: Context, hourOfDay: Int = 20) {
        ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNext(hourOfDay), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        ioScope.launch {
            context.reminderDataStore.edit { it[ENABLED_KEY] = true }
        }
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        ioScope.launch {
            context.reminderDataStore.edit { it[ENABLED_KEY] = false }
        }
    }

    fun isEnabledFlow(context: Context): Flow<Boolean> =
        context.reminderDataStore.data.map { it[ENABLED_KEY] ?: false }

    private fun millisUntilNext(hourOfDay: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hourOfDay).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return ChronoUnit.MILLIS.between(
            now.atZone(ZoneId.systemDefault()),
            target.atZone(ZoneId.systemDefault()),
        )
    }

    internal fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily check-in",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Your daily VibeCheck nudge." }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}

class ReminderWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success() // silently skip without permission
        }
        ReminderScheduler.ensureChannel(ctx)
        val contentIntent = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time for a VibeCheck")
            .setContentText("30 seconds. How are you, really?")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(0x1000, notification)
        return Result.success()
    }
}
