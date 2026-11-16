package com.periodtracker.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.periodtracker.JournalApplication
import com.periodtracker.MainActivity
import com.periodtracker.R
import com.periodtracker.domain.*
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

data class DueReminder(val key: String, val daily: Boolean)
object ReminderPolicy {
    fun due(snapshot: JournalSnapshot, now: LocalDateTime): List<DueReminder> {
        val prefs = snapshot.preferences
        if (!prefs.onboardingComplete || now.toLocalTime() < prefs.reminderTime) return emptyList()
        val date = now.toLocalDate()
        return buildList {
            if (prefs.dailyEnabled && snapshot.logs.none { it.date == date }) add(DueReminder("daily:$date", true))
            val estimate = CyclePredictionEngine().predict(snapshot.periods, prefs.cycle)
            if (prefs.upcomingEnabled && snapshot.periods.none { it.status == PeriodStatus.ONGOING } && estimate != null &&
                estimate.centralStart.minusDays(prefs.leadDays.toLong()) == date) add(DueReminder("upcoming:${estimate.centralStart}", false))
        }
    }
}
class ReminderScheduler(private val context: Context) {
    private val work get() = WorkManager.getInstance(context)
    fun schedule(snapshot: JournalSnapshot) {
        if (!snapshot.preferences.onboardingComplete || (!snapshot.preferences.dailyEnabled && !snapshot.preferences.upcomingEnabled)) { cancel(); return }
        work.enqueueUniquePeriodicWork("journal-reminders", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build())
    }
    fun checkNow() { work.enqueueUniqueWork("journal-reminder-check", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<ReminderWorker>().build()) }
    fun cancel() { work.cancelUniqueWork("journal-reminders"); work.cancelUniqueWork("journal-reminder-check"); NotificationManagerCompat.from(context).cancelAll() }
}
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) return Result.success()
        val repository = (context as JournalApplication).container.repository
        return try {
            val snapshot = repository.snapshot()
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("journal", context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
            for (reminder in ReminderPolicy.due(snapshot, LocalDateTime.now())) {
                if (!repository.claimReminder(reminder.key)) continue
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_today", reminder.daily)
                }
                val pending = PendingIntent.getActivity(context, if (reminder.daily) 1 else 2, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val body = if (snapshot.preferences.discreet) R.string.reminder_discreet
                    else if (reminder.daily) R.string.reminder_daily else R.string.reminder_upcoming
                manager.notify(if (reminder.daily) 1 else 2, NotificationCompat.Builder(context, "journal")
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.reminder_title))
                    .setContentText(context.getString(body)).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentIntent(pending).setAutoCancel(true).build())
            }
            Result.success()
        } catch (_: java.io.IOException) { Result.retry() }
    }
}
class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED)) {
            ReminderScheduler(context).checkNow()
        }
    }
}
